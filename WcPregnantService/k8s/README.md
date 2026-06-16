# Kubernetes Deployment Guide for WC Pregnant Service

This directory contains production-ready Kubernetes manifests for deploying the WC Pregnant application.

## Architecture Overview

- **Namespace**: `wcpregnant` (isolated environment)
- **App**: 2-5 replicas (autoscaling based on CPU 70% threshold)
- **Database**: PostgreSQL 15 with persistent storage (10Gi)
- **Networking**: Service (ClusterIP:80), Ingress (nginx), NetworkPolicy
- **Health Checks**: Liveness & Readiness probes
- **HA**: PodDisruptionBudget (minimum 1 pod available)

## Prerequisites

- Kubernetes cluster (v1.20+)
- `kubectl` installed and configured
- (Optional) cert-manager for TLS (Ingress manifest assumes Let's Encrypt)
- (Optional) nginx-ingress-controller
- (Optional) metrics-server for HPA

## Quick Start

### 1. Create the namespace

```bash
kubectl apply -f namespace.yaml
```

### 2. Create secrets and configuration

```bash
kubectl apply -f secret.yaml -n wcpregnant
kubectl apply -f configmap.yaml -n wcpregnant
```

### 3. Deploy PostgreSQL

```bash
kubectl apply -f postgres-pvc.yaml -n wcpregnant
kubectl apply -f postgres-deployment.yaml -n wcpregnant
```

### 4. Deploy the application

```bash
kubectl apply -f deployment.yaml -n wcpregnant
kubectl apply -f service.yaml -n wcpregnant
```

### 5. Enable autoscaling

```bash
kubectl apply -f hpa.yaml -n wcpregnant
```

### 6. Configure networking

```bash
kubectl apply -f networkpolicy.yaml -n wcpregnant
kubectl apply -f pdb.yaml -n wcpregnant
```

### 7. (Optional) Setup Ingress for external access

Edit `ingress.yaml` to replace `wcpregnant.example.com` with your domain, then:

```bash
kubectl apply -f ingress.yaml -n wcpregnant
```

## One-liner deployment (all resources)

```bash
kubectl apply -f . -n wcpregnant
```

Note: This requires all manifests to be in the same directory.

## Verify Deployment

```bash
kubectl get pods -n wcpregnant
kubectl get services -n wcpregnant
kubectl describe deployment wcpregnant-deployment -n wcpregnant
kubectl logs -f deployment/wcpregnant-deployment -n wcpregnant
```

## Access the Application

### Port-forward (for testing)

```bash
kubectl port-forward -n wcpregnant svc/wcpregnant-service 8080:80
curl http://localhost:8080/api/
```

### Via Ingress (production)

Once Ingress is configured with your domain and DNS is pointing to your ingress IP:

```bash
curl https://wcpregnant.example.com/api/
```

## Monitoring and Observability

### Check Pod autoscaling

```bash
kubectl get hpa -n wcpregnant
kubectl describe hpa wcpregnant-hpa -n wcpregnant
```

### View resource usage

```bash
kubectl top pods -n wcpregnant
kubectl top nodes
```

### Check health probes

```bash
kubectl describe pod <pod-name> -n wcpregnant
```

Look for `Readiness` and `Liveness` probe status.

## Update Application

When you build a new image:

```bash
kubectl set image deployment/wcpregnant-deployment wcpregnant=ghcr.io/<OWNER>/wcpregnant:latest -n wcpregnant
```

## Scaling

### Manual scaling

```bash
kubectl scale deployment wcpregnant-deployment --replicas=3 -n wcpregnant
```

### Autoscaling status

HPA will automatically scale between 2-5 replicas based on CPU utilization (target: 70%).

## Troubleshooting

### Pod stuck in pending

```bash
kubectl describe pod <pod-name> -n wcpregnant
```

Check events and resource constraints.

### Application not responding

```bash
kubectl logs deployment/wcpregnant-deployment -n wcpregnant
kubectl exec -it <pod-name> -n wcpregnant -- /bin/sh
```

### Database connection issues

```bash
kubectl logs deployment/postgres -n wcpregnant
kubectl exec -it postgres-<pod-id> -n wcpregnant -- psql -U wcpregnant -d wcpregnant
```

## Security Best Practices Applied

- **Secrets**: Database credentials stored in `Secret` (not in ConfigMap)
- **NetworkPolicy**: Restricts pod-to-pod communication
- **RBAC**: Configure role-based access control (add later)
- **TLS/HTTPS**: Enabled via cert-manager + nginx-ingress
- **Resource Limits**: CPU/Memory requests and limits set
- **ReadOnly filesystem**: (Optional) Can be added to deployment
- **Non-root users**: (Optional) Configure in Dockerfile via USER directive

## Production Checklist

- [ ] Replace `<OWNER>` in deployment.yaml with your registry
- [ ] Update domain in ingress.yaml
- [ ] Configure cert-manager for TLS
- [ ] Set up monitoring/logging (Prometheus, ELK, etc.)
- [ ] Configure backups for PostgreSQL PV
- [ ] Test disaster recovery
- [ ] Set resource quotas and limits per namespace
- [ ] Enable audit logging
- [ ] Configure pod security policies / standards

## Cleanup

```bash
kubectl delete namespace wcpregnant
```

This will delete all resources in the namespace.

