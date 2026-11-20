> 📂 **Documentation** — [index](../README.md) · Phase **5 — Déploiement** · Manifestes réels : [`WcPregnantService/k8s/`](../../WcPregnantService/k8s/)

# Guide de déploiement Kubernetes — WcPregnant Service

Ce guide documente les manifestes situés dans `WcPregnantService/k8s/`.

## 1. Vue d'ensemble

| Élément | Configuration |
|---|---|
| **Espace de noms** | `wcpregnant` (environnement isolé) |
| **Application** | 2 à 5 réplicas (autoscaling selon le seuil CPU de 70 %) |
| **Base de données** | PostgreSQL 15 avec stockage persistant (10 Gi) |
| **Réseau** | Service (`ClusterIP:80`), Ingress (nginx), `NetworkPolicy` |
| **Contrôles de santé** | Sondes de survie (*liveness*) et de disponibilité (*readiness*) |
| **Haute disponibilité** | `PodDisruptionBudget` (au moins 1 pod disponible) |

## 2. Prérequis

- Un cluster Kubernetes (v1.20+) ;
- `kubectl` installé et configuré ;
- (Optionnel) cert-manager pour le TLS — le manifeste Ingress suppose Let's Encrypt ;
- (Optionnel) nginx-ingress-controller ;
- **metrics-server** pour `kubectl top` et l'HPA : le manifeste local [`metrics-server.yaml`](../../WcPregnantService/k8s/metrics-server.yaml) est **obligatoire** dans le cluster Docker Desktop, le `components.yaml` upstream ne fonctionne pas ici (voir §7.2).

## 3. Démarrage rapide

### 3.1 Créer l'espace de noms

```bash
kubectl apply -f namespace.yaml
```

### 3.2 Créer les secrets et la configuration

```bash
kubectl apply -f secret.yaml -n wcpregnant
kubectl apply -f configmap.yaml -n wcpregnant
```

### 3.3 Déployer PostgreSQL

```bash
kubectl apply -f postgres-pvc.yaml -n wcpregnant
kubectl apply -f postgres-deployment.yaml -n wcpregnant
```

### 3.4 Déployer l'application

```bash
kubectl apply -f deployment.yaml -n wcpregnant
kubectl apply -f service.yaml -n wcpregnant
```

### 3.5 Activer l'autoscaling

```bash
kubectl apply -f hpa.yaml -n wcpregnant
```

### 3.6 Configurer le réseau

```bash
kubectl apply -f networkpolicy.yaml -n wcpregnant
kubectl apply -f pdb.yaml -n wcpregnant
```

### 3.7 (Optionnel) Configurer l'Ingress pour un accès externe

Remplacer `wcpregnant.example.com` par votre domaine dans `ingress.yaml`, puis :

```bash
kubectl apply -f ingress.yaml -n wcpregnant
```

## 4. Déploiement global (toutes les ressources)

```bash
kubectl apply -f . -n wcpregnant
```

> Nécessite que tous les manifestes se trouvent dans le même répertoire.

## 5. Vérifier le déploiement

```bash
kubectl get pods -n wcpregnant
kubectl get services -n wcpregnant
kubectl describe deployment wcpregnant-deployment -n wcpregnant
kubectl logs -f deployment/wcpregnant-deployment -n wcpregnant
```

## 6. Accéder à l'application

### Transfert de port (pour les tests)

```bash
kubectl port-forward -n wcpregnant svc/wcpregnant-service 14007:80
curl http://localhost:14007/api/
```

### Via l'Ingress (production)

Une fois l'Ingress configuré avec votre domaine et le DNS pointant vers l'IP de l'Ingress :

```bash
curl https://wcpregnant.example.com/api/
```

## 7. Monitoring et observabilité

### Vérifier l'autoscaling des pods

```bash
kubectl get hpa -n wcpregnant
kubectl describe hpa wcpregnant-hpa -n wcpregnant
```

### Consulter l'utilisation des ressources

```bash
kubectl top pods -n wcpregnant
kubectl top nodes
```

### 7.1 Vérifier l'enregistrement du groupe d'API `metrics.k8s.io`

```bash
kubectl get apiservice v1beta1.metrics.k8s.io
kubectl get --raw "/apis/metrics.k8s.io/v1beta1/nodes"
```

Depuis **Kubernetes 1.34**, le groupe d'API extension `metrics.k8s.io` n'est plus
découvert automatiquement à partir du nom du Service : il faut déclarer **explicitement**
un objet `APIService` (section 1 de `metrics-server.yaml`). Sans lui, `kubectl top`
renvoie `error: the metrics API is not available`.

### 7.2 Pourquoi le manifeste local diffère du `components.yaml` upstream

| Point | Manifeste upstream | `metrics-server.yaml` (cluster local) | Motif |
|---|---|---|---|
| `APIService` | absent | présent (`v1beta1.metrics.k8s.io`) | obligatoire depuis Kubernetes 1.34 pour enregistrer le groupe d'API |
| RBAC | `nodes`, `pods` | ajoute `nodes/metrics` et `nodes/status` | le kubelet expose les métriques sous un **sous-ressource** : sans lui le scraper reçoit un `403` |
| Sondes | HTTP GET `/` | `tcpSocket` sur `443` | metrics-server n'expose **aucun chemin non authentifié** : une sonde HTTP renvoie `403` et fait crasher le pod |
| Service | sans sélecteur | `selector: k8s-app: metrics-server` | sans sélecteur le Service n'a **aucun endpoint**, l'agrégateur ne peut pas joindre le serveur |
| TLS | — | `insecureSkipTLSVerify: true` | le serveur utilise un certificat auto-signé (`CN=localhost`) : l'agrégateur de l'API server refuse la vérification en environnement local |
| Scraping kubelet | — | `--kubelet-insecure-tls` | certificats auto-signés du kubelet de Docker Desktop |

> `insecureSkipTLSVerify` est réservé aux environnements de développement. En
> production, fournir un certificat valide à metrics-server (`--cert-dir` +
> `--kubelet-certificate-authority`) et retirer cette ligne.

### 7.3 Diagnostic rapide si `kubectl top` échoue

```bash
kubectl get pods -n kube-system -l k8s-app=metrics-server     # pod Running ?
kubectl logs -n kube-system -l k8s-app=metrics-server         # "unable to connect to kubelet" / 403 ?
kubectl get endpoints -n kube-system metrics-server           # endpoints vides ?
kubectl get apiservice v1beta1.metrics.k8s.io                 # Available: True ?
```


### Vérifier les sondes de santé

```bash
kubectl describe pod <nom-du-pod> -n wcpregnant
```

Rechercher l'état des sondes `readiness` et `liveness`.

## 8. Mettre à jour l'application

Après la construction d'une nouvelle image :

```bash
kubectl set image deployment/wcpregnant-deployment wcpregnant=ghcr.io/<OWNER>/wcpregnant:latest -n wcpregnant
```

## 9. Scalabilité

### Mise à l'échelle manuelle

```bash
kubectl scale deployment wcpregnant-deployment --replicas=3 -n wcpregnant
```

### Autoscaling

L'HPA fait varier automatiquement le nombre de réplicas entre 2 et 5 selon l'utilisation CPU (cible : 70 %).

## 10. Diagnostic

### Pod bloqué en état `Pending`

```bash
kubectl describe pod <nom-du-pod> -n wcpregnant
```

Consulter les événements et les contraintes de ressources.

### Application sans réponse

```bash
kubectl logs deployment/wcpregnant-deployment -n wcpregnant
kubectl exec -it <nom-du-pod> -n wcpregnant -- /bin/sh
```

### Problèmes de connexion à la base

```bash
kubectl logs deployment/postgres -n wcpregnant
kubectl exec -it postgres-<id-du-pod> -n wcpregnant -- psql -U wcpregnant -d wcpregnant
```

## 11. Bonnes pratiques de sécurité appliquées

| Pratique | Mise en œuvre |
|---|---|
| **Secrets** | Identifiants de base de données dans un `Secret` (et non dans le `ConfigMap`) |
| **NetworkPolicy** | Restreint la communication entre pods |
| **RBAC** | À configurer (prévu, non encore ajouté) |
| **TLS/HTTPS** | Activé via cert-manager + nginx-ingress |
| **Limites de ressources** | Demandes et limites CPU/mémoire définies |
| **Système de fichiers en lecture seule** | Optionnel, peut être ajouté au déploiement |
| **Utilisateur non root** | Déjà appliqué dans le `Dockerfile` (`USER appuser`) |

> ⚠️ `k8s/secret.yaml` contient des mots de passe **de démonstration**. En production, injecter de vrais secrets via les variables d'environnement ou un gestionnaire de secrets, jamais dans le dépôt.

## 12. Checklist de mise en production

- [ ] Remplacer `<OWNER>` dans `deployment.yaml` par votre registre ;
- [ ] Mettre à jour le domaine dans `ingress.yaml` ;
- [ ] Configurer cert-manager pour le TLS ;
- [ ] Mettre en place le monitoring / les logs (Prometheus, ELK, etc.) ;
- [ ] Configurer les sauvegardes du volume persistant PostgreSQL ;
- [ ] Tester la reprise après sinistre ;
- [ ] Définir les quotas et limites de ressources par espace de noms ;
- [ ] Activer la journalisation d'audit ;
- [ ] Appliquer les politiques de sécurité des pods.

## 13. Nettoyage

```bash
kubectl delete namespace wcpregnant
```

> Cela supprime **toutes** les ressources de l'espace de noms.

