# Kubernetes Deployment Script for Windows PowerShell

$NAMESPACE = "wcpregnant"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "WC Pregnant Service - Kubernetes Deployment Script" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host ""

# Check if kubectl is available
try {
    kubectl config current-context | Out-Null
} catch {
    Write-Host "ERROR: kubectl is not configured. Please set up your kubeconfig." -ForegroundColor Red
    exit 1
}

$context = kubectl config current-context
Write-Host "Current Kubernetes context: $context" -ForegroundColor Yellow
Write-Host "Target namespace: $NAMESPACE" -ForegroundColor Yellow
Write-Host ""

$response = Read-Host "Continue? (y/n)"
if ($response -ne 'y' -and $response -ne 'Y') {
    exit 1
}

Write-Host ""
Write-Host "Step 1: Creating namespace..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\namespace.yaml"

Write-Host "Step 2: Creating secrets and configuration..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\secret.yaml" -n $NAMESPACE
kubectl apply -f "$SCRIPT_DIR\configmap.yaml" -n $NAMESPACE

Write-Host "Step 3: Creating PostgreSQL PVC..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\postgres-pvc.yaml" -n $NAMESPACE

Write-Host "Step 4: Deploying PostgreSQL..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\postgres-deployment.yaml" -n $NAMESPACE

Write-Host "Waiting for PostgreSQL to be ready (max 300s)..." -ForegroundColor Yellow
kubectl wait --for=condition=available --timeout=300s deployment/postgres -n $NAMESPACE

Write-Host "Step 5: Deploying application..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\deployment.yaml" -n $NAMESPACE
kubectl apply -f "$SCRIPT_DIR\service.yaml" -n $NAMESPACE

Write-Host "Waiting for application to be ready (max 300s)..." -ForegroundColor Yellow
kubectl wait --for=condition=available --timeout=300s deployment/wcpregnant-deployment -n $NAMESPACE

Write-Host "Step 6: Setting up autoscaling..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\hpa.yaml" -n $NAMESPACE

Write-Host "Step 7: Configuring networking..." -ForegroundColor Green
kubectl apply -f "$SCRIPT_DIR\networkpolicy.yaml" -n $NAMESPACE
kubectl apply -f "$SCRIPT_DIR\pdb.yaml" -n $NAMESPACE

Write-Host "Step 8: (Optional) Setting up Ingress..." -ForegroundColor Green
$deployIngress = Read-Host "Deploy Ingress? (y/n)"
if ($deployIngress -eq 'y' -or $deployIngress -eq 'Y') {
    kubectl apply -f "$SCRIPT_DIR\ingress.yaml" -n $NAMESPACE
    Write-Host "Ingress deployed. Update your DNS records to point to the ingress IP." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "Deployment complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Verify deployment:" -ForegroundColor Cyan
Write-Host "  kubectl get pods -n $NAMESPACE" -ForegroundColor Yellow
Write-Host "  kubectl get services -n $NAMESPACE" -ForegroundColor Yellow
Write-Host ""
Write-Host "Access application (port-forward):" -ForegroundColor Cyan
Write-Host "  kubectl port-forward -n $NAMESPACE svc/wcpregnant-service 14007:80" -ForegroundColor Yellow
Write-Host ""

