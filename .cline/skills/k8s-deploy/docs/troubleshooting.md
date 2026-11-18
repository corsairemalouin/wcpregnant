# Diagnostic Kubernetes

| Symptôme | Cause probable | Commande de vérification |
|---|---|---|
| `ImagePullBackOff` | image absente du registry / tag incorrect | `kubectl describe pod <pod> -n wcpregnant` |
| Pod `CrashLoopBackOff` | DSN PostgreSQL invalide, secret manquant | `kubectl logs <pod> -n wcpregnant` |
| Probe `readiness` échouée | Actuator inaccessible, chemin `/actuator/health/readiness` (et non `/api/actuator/...`) | `kubectl exec <pod> -- wget -qO- http://localhost:8080/actuator/health` |
| Service injoignable | Service/Ingress non appliqué, NetworkPolicy restrictive | `kubectl get endpoints -n wcpregnant` |
| HPA inactif | metrics-server absent | `kubectl top pods -n wcpregnant` |
| PDB bloque le drain | `minAvailable` trop élevé pour le nombre de nœuds | `kubectl describe pdb -n wcpregnant` |

## Ordre de contrôle

1. `kubectl get pods -n wcpregnant` — état des pods
2. `kubectl describe pod <pod> -n wcpregnant` — événements et image
3. `kubectl logs <pod> -n wcpregnant` — stack trace Spring
4. `kubectl get events -n wcpregnant --sort-by=.lastTimestamp`
5. Vérifier ConfigMap/Secret : `kubectl get configmap,secret -n wcpregnant`

## File d'attente de build

Le build Docker est long (Maven dans l'image). Pour itérer rapidement :

```powershell
mvn -DskipTests package
docker build -t wcpregnant:local .
docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wcpregnant wcpregnant:local
```
