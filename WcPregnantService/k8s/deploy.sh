#!/bin/bash

set -e

NAMESPACE="wcpregnant"
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "WC Pregnant Service - Kubernetes Deployment Script"
echo "===================================================="
echo ""

if [ -z "$(kubectl config current-context)" ]; then
    echo "ERROR: kubectl is not configured. Please set up your kubeconfig."
    exit 1
fi

echo "Current Kubernetes context: $(kubectl config current-context)"
echo "Target namespace: $NAMESPACE"
echo ""

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

echo "Step 1: Creating namespace..."
kubectl apply -f "$SCRIPT_DIR/namespace.yaml"

echo "Step 2: Creating secrets and configuration..."
kubectl apply -f "$SCRIPT_DIR/secret.yaml" -n $NAMESPACE
kubectl apply -f "$SCRIPT_DIR/configmap.yaml" -n $NAMESPACE

echo "Step 3: Creating PostgreSQL PVC..."
kubectl apply -f "$SCRIPT_DIR/postgres-pvc.yaml" -n $NAMESPACE

echo "Step 4: Deploying PostgreSQL..."
kubectl apply -f "$SCRIPT_DIR/postgres-deployment.yaml" -n $NAMESPACE

echo "Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/postgres -n $NAMESPACE

echo "Step 5: Deploying application..."
kubectl apply -f "$SCRIPT_DIR/deployment.yaml" -n $NAMESPACE
kubectl apply -f "$SCRIPT_DIR/service.yaml" -n $NAMESPACE

echo "Waiting for application to be ready..."
kubectl wait --for=condition=available --timeout=300s deployment/wcpregnant-deployment -n $NAMESPACE

echo "Step 6: Setting up autoscaling..."
kubectl apply -f "$SCRIPT_DIR/hpa.yaml" -n $NAMESPACE

echo "Step 7: Configuring networking..."
kubectl apply -f "$SCRIPT_DIR/networkpolicy.yaml" -n $NAMESPACE
kubectl apply -f "$SCRIPT_DIR/pdb.yaml" -n $NAMESPACE

echo "Step 8: (Optional) Setting up Ingress..."
read -p "Deploy Ingress? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl apply -f "$SCRIPT_DIR/ingress.yaml" -n $NAMESPACE
    echo "Ingress deployed. Update your DNS records to point to the ingress IP."
fi

echo ""
echo "===================================================="
echo "Deployment complete!"
echo ""
echo "Verify deployment:"
echo "  kubectl get pods -n $NAMESPACE"
echo "  kubectl get services -n $NAMESPACE"
echo ""
echo "Access application (port-forward):"
echo "  kubectl port-forward -n $NAMESPACE svc/wcpregnant-service 14007:80"
echo ""

