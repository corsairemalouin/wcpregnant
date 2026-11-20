> 📂 **Documentation** — [index](../README.md) · Phase **5 — Déploiement** · Pré-requis : [environnement-developement.md](environnement-developement.md) · Build Docker : [build-et-deploiement.md](build-et-deploiement.md)

# Scripts de démarrage et d'arrêt complet de la solution

Quatre scripts à la **racine du monorepo** orchestrent les trois composants de la solution
(base de données, API, frontend) et leur arrêt, sans avoir à lancer chaque module séparément.

| Fichier | Plateforme | Rôle |
|---|---|---|
| [`start.ps1`](../../start.ps1) | Windows / PowerShell | Démarrage complet : PostgreSQL (Docker) → API Spring Boot → frontend Angular |
| [`stop.ps1`](../../stop.ps1) | Windows / PowerShell | Arrêt complet : frontend, API, conteneurs Docker |
| [`start.sh`](../../start.sh) | Linux / macOS (bash) | Idem, version shell |
| [`stop.sh`](../../stop.sh) | Linux / macOS (bash) | Idem, version shell |
| [`.env.example`](../../.env.example) | — | Variables d'environnement de référence (inférence, base, ports) |

Les scripts enregistrent leur état dans **`.run/stack.state`** (PID des processus, ports, mode)
et les logs de session dans `.run/backend.log`, `.run/frontend.log`. Le dossier `.run/` est
ignoré par Git.

---

## 1. Démarrer la solution

### Windows (PowerShell)

```powershell
cd D:\Programmation\wcpregnant
.\start.ps1
```

### Linux / macOS

```bash
cd /chemin/du/projet
chmod +x start.sh stop.sh   # une seule fois
./start.sh
```

### Ce que fait le script, dans l'ordre

1. **Vérifie les pré-requis** : `npm` est obligatoire, `mvn` l'est en mode `local`. Docker est
   *optionnel* : le script distingue « Docker installé » de « daemon Docker opérationnel »
   (`docker info`) et bascule sur H2 si le daemon n'est pas démarré. En mode `docker`, l'absence
   de daemon est une **erreur bloquante**.
2. **Démarre PostgreSQL** via `docker compose -f WcPregnantService/docker-compose.yml -p wcpregnant up -d db`,
   puis attend que `pg_isready` réponde (30 essais, 2 s d'intervalle).
3. **Injecte la configuration** de la base (`SPRING_DATASOURCE_*`) et de l'inférence agentique
   (`AGENT_INFERENCE_BASE_URL`, `AGENT_MODEL`).
4. **Démarre l'API** : `mvn spring-boot:run` sur le port de l'API avec le profil `dev`
   (mode `local`) ou `docker compose up -d` (mode `docker`), puis attend que le port soit ouvert.
5. **Démarre le frontend** : `ng serve --port 4200 --proxy-config src/proxy.conf.json`
   (`npm install` est déclenché si `node_modules` est absent).
6. **Écrit l'état** dans `.run/stack.state` et affiche les URLs d'accès.

---

## 2. Options disponibles

| Option | `start.ps1` | `start.sh` | Défaut | Effet |
|---|---|---|---|---|
| Mode | `-Mode local\|docker` | `--mode local\|docker` | `local` | `local` = Maven + `ng serve` ; `docker` = API et base via docker-compose |
| Profil Spring | `-Profile` | `--profile` | `dev` | `dev` active les utilisateurs de démonstration (`admin`/`admin`, `manu`/`manu`) ; `prod` s'appuie sur la table `users` |
| Port API | `-ApiPort` | `--api-port` | `14007` | Port **dédié au projet**, aligné sur `WcPregnantApp/src/proxy.conf.json` |
| Port frontend | `-FrontPort` | `--front-port` | `4200` | Port d'`ng serve` |
| Inférence | `-InferenceUrl` / `-InferenceModel` | `--inference-url` / `--inference-model` | `http://localhost:8000/v1` / `Qwen-3.8-Flash-Next` | Serveur d'inférence **local** de la couche agentique |
| Sans base | `-NoDb` | `--no-db` | — | Skip PostgreSQL : Spring utilise H2 en mémoire |
| Sans frontend | `-NoFront` | `--no-front` | — | API seule (utile pour Bruno / curl) |
| Simulation | `-DryRun` | `--dry-run` | — | Affiche le plan sans lancer aucun processus |

`stop.ps1` / `stop.sh` acceptent `-ApiPort`/`--api-port`, `-FrontPort`/`--front-port`,
`-PurgeVolumes`/`--purge-volumes` (suppression du volume `db_data`) et `-Status`/`--status`
(état de la stack sans arrêt).

---

## 3. Arrêter la solution

```powershell
.\stop.ps1                  # arrêt propre
.\stop.ps1 -PurgeVolumes    # arrêt + suppression des données PostgreSQL
.\stop.ps1 -Status          # état de la stack (ports en écoute, conteneurs)
```

```bash
./stop.sh                    # arrêt propre
./stop.sh --purge-volumes    # arrêt + suppression des données
./stop.sh --status           # état de la stack
```

L'arrêt est **défensif** : le script tue d'abord les PID enregistrés, puis tout processus
encore en écoute sur les ports de l'API et du frontend (enfants `java` / `node`), puis les
conteneurs du projet compose `wcpregnant`.

---

## 4. Ports et URLs

| Composant | Port | URL |
|---|---|---|
| Frontend Angular | 4200 | <http://localhost:4200> |
| API Spring Boot (mode local) | 14007 | <http://localhost:14007> |
| API Spring Boot (mode docker) | 14007 (via `API_PORT`) | <http://localhost:14007> |
| Santé API | — | `/actuator/health` — **publique** par conception (chaîne `healthFilterChain`, `@Order(0)`) : `200 {"status":"UP"}` sans authentification, utilisée par le `HEALTHCHECK` Docker et les probes Kubernetes. Les scripts testent toujours la disponibilité par le **port** |
| Endpoints protégés (Basic) | — | `/toilets/**`, `/api/**`, `/agent/**` — `admin` / `admin` (rôle `ADMIN`) ou `manu` / `manu` (rôle `USER`) en profil `dev` |
| Outils agentiques | — | `GET /agent/tools` (Basic `admin` / `admin`) |
| PostgreSQL | 5432 | `jdbc:postgresql://localhost:5432/wcpregnant` |
| Inférence locale (Strata AI) | 8000 | <http://localhost:8000/v1> |

> `docker-compose.yml` publie l'API sur `${API_PORT:-14007}` : le port **14007** est
> dédié au projet — il n'est ni un port générique (8080, 8081) ni un port de la plage
> éphémère Windows (49152-65535), ce qui supprime les interférences avec d'autres services
> locaux. `application.properties` définit le même défaut via `server.port=${API_PORT:14007}`.
>
> **Mesure sur ce poste** (`netstat -an | Select-String LISTENING`) : `8080` est occupé
> (IPv4 et IPv6) par une autre application locale, `5432` par PostgreSQL. `14007` est
> libre, dans la bande 11000-49151. Les scripts refusent de démarrer si le port choisi
> n'est pas libre (`PortFree` / `port_free`).

---

## 5. Comportements de robustesse

| Situation | Comportement |
|---|---|
| Docker absent ou daemon non démarré | PostgreSQL n'est pas lancé, l'API démarre sur **H2 en mémoire** ; un avertissement est affiché |
| Port déjà occupé | Le composant n'est **pas relancé** : le script signale que le service est déjà disponible |
| Serveur d'inférence injoignable | Avertissement ; `/agent/chat` répondra **503**, le CRUD reste utilisable |
| `node_modules` absent | `npm install` est exécuté automatiquement avant `ng serve` |
| Arrêt sans `.run/stack.state` | Les processus sont identifiés par port ; aucun arrêt ne reste orphelin |

---

## 6. Pièges techniques évités (robustesse réelle des scripts)

| Script | Piège | Solution retenue |
|---|---|---|
| `start.sh`, `stop.sh` | `set -euo pipefail` : une construction courte du type `[[ cond ]] && action` renvoie `1` quand la condition est fausse, ce qui **interrompt le script** (exit 1) alors qu'il ne s'agit pas d'une erreur | Toutes les conditions sont écrites avec des blocs `if … then … else … fi` explicites (ex. `save_state()`, `docker_ready()`) |
| `start.sh`, `stop.sh` | `curl`/`docker` écrivent sur `stderr` et sortent en code non nul quand le service est absent | Les appels sont redirigés (`>/dev/null 2>&1`) et leur code de retour est testé explicitement |
| `start.ps1`, `stop.ps1` | `$ErrorActionPreference = 'Stop'` transforme la `stderr` d'une commande **native** (`docker info`, `docker compose`) en `RemoteException` : le script s'arrête alors que Docker est simplement indisponible | Chaque appel Docker est encapsulé dans un `try { … } catch { … }` ; `DockerReady()` teste le **daemon** (`docker info`) et non la présence du binaire |
| tous | Code de sortie ambigu (hérité de la dernière commande) | Les scripts se terminent par `exit 0` (ou `exit 1` en cas d'erreur bloquante réelle) |

**Vérifications effectuées** (Docker daemon non démarré sur la machine de développement) :

| Commande | Résultat |
|---|---|
| `bash -n start.sh && bash -n stop.sh` | syntaxe OK |
| `bash start.sh --no-front` | API démarrée sur 14007 (H2 en mémoire), exit `0` |
| `bash stop.sh` | processus arrêtés, exit `0` |
| `bash stop.sh --status` | état affiché, exit `0` |
| `.\start.ps1 -NoFront` | API démarrée sur 14007 (H2 en mémoire), exit `0` |
| `.\stop.ps1` | processus arrêtés, exit `0` |
| `.\stop.ps1 -Status` | état affiché, exit `0` |
| `.\start.ps1 -DryRun` | plan affiché sans exécution, exit `0` |

