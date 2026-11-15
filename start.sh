#!/usr/bin/env bash
# Demarrage complet de la solution WcPregnant :
#   PostgreSQL (docker-compose) + API Spring Boot + frontend Angular.
#
#   ./start.sh                          # mode local (Maven + ng serve)
#   ./start.sh --mode docker            # API + base via docker-compose
#   ./start.sh --no-db --no-front       # API seule
#   ./start.sh --dry-run                # afficher le plan sans lancer
#
# Les PID sont ecrits dans .run/stack.state ; stop.sh les arrete.
set -euo pipefail

MODE="local"
PROFILE="dev"
API_PORT=8081
FRONT_PORT=4200
INFERENCE_URL="${AGENT_INFERENCE_BASE_URL:-http://localhost:8000/v1}"
INFERENCE_MODEL="${AGENT_MODEL:-Qwen-3.8-Flash-Next}"
NO_DB=0
NO_FRONT=0
DRY_RUN=0

usage() {
  cat <<'EOF'
Usage : ./start.sh [options]
  --mode local|docker     local = Maven + ng serve (defaut), docker = docker-compose
  --profile NAME          profil Spring (defaut dev : utilisateurs admin/admin et manu/manu)
  --api-port PORT         port de l API (defaut 8081, aligne sur src/proxy.conf.json)
  --front-port PORT       port du frontend (defaut 4200)
  --inference-url URL     serveur d inférence local (defaut http://localhost:8000/v1)
  --inference-model NAME  modele expose par Strata AI
  --no-db                 ne pas demarrer PostgreSQL (H2 en memoire)
  --no-front              ne pas demarrer le frontend Angular
  --dry-run               afficher le plan sans executer
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --mode) MODE="$2"; shift 2 ;;
    --profile) PROFILE="$2"; shift 2 ;;
    --api-port) API_PORT="$2"; shift 2 ;;
    --front-port) FRONT_PORT="$2"; shift 2 ;;
    --inference-url) INFERENCE_URL="$2"; shift 2 ;;
    --inference-model) INFERENCE_MODEL="$2"; shift 2 ;;
    --no-db) NO_DB=1; shift ;;
    --no-front) NO_FRONT=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Option inconnue : $1" >&2; usage; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.run"
STATE="$RUN_DIR/stack.state"
COMPOSE="$ROOT/WcPregnantService/docker-compose.yml"
PROJECT="wcpregnant"
DB_CONTAINER="wcpregnant-db"
DB="none"
API_PID=""
FRONT_PID=""

info() { echo "  $1"; }
ok()   { echo "  [OK] $1"; }
warn() { echo "  [!!] $1"; }
fail() { echo "  [XX] $1" >&2; exit 1; }

docker_ready() {
  command -v docker >/dev/null && docker info >/dev/null 2>&1
}

port_free() {
  # un port est considere libre si rien n y est en ecoute TCP
  ! port_up "$1"
}

port_up() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null && return 0
  return 1
}

wait_port() {
  for _ in $(seq 1 "$2"); do
    if port_up "$1"; then return 0; fi
    sleep 1
  done
  return 1
}

wait_http() {
  for _ in $(seq 1 "$2"); do
    if curl -fsS -o /dev/null -m 3 "$1"; then return 0; fi
    sleep 1
  done
  return 1
}

save_state() {
  if [[ $DRY_RUN -eq 1 ]]; then
    return 0
  fi
  mkdir -p "$RUN_DIR"
  {
    echo "mode=$MODE"
    echo "api_port=$API_PORT"
    echo "front_port=$FRONT_PORT"
    echo "db=$DB"
    if [[ -n "$API_PID" ]]; then echo "api_pid=$API_PID"; fi
    if [[ -n "$FRONT_PID" ]]; then echo "front_pid=$FRONT_PID"; fi
  } > "$STATE"
  return 0
}

echo '=========================================================='
echo " WcPregnant - demarrage de la solution (mode $MODE)"
echo '=========================================================='
if [[ $DRY_RUN -eq 1 ]]; then warn 'Mode sec (--dry-run) : aucune commande ne sera lancee'; fi

# --- 1. Preconditions ---------------------------------------------------------
echo '  Verification des pre requis'
if [[ $MODE == local ]] && ! command -v mvn >/dev/null; then fail 'Maven (mvn) est requis'; fi
if ! command -v npm >/dev/null; then fail 'Node.js / npm est requis'; fi
if ! docker_ready; then warn 'Docker indisponible (daemon non demarre) : PostgreSQL ne sera pas demarre, Spring bascule sur H2 en memoire'; fi
if [[ $MODE == docker ]] && ! docker_ready; then fail 'Mode docker : un daemon Docker operationnel est requis'; fi
echo '  [OK] Pre requis OK'

# --- 2. Base de donnees PostgreSQL ------------------------------------------
if [[ $NO_DB -eq 1 ]]; then
  warn 'Base de donnees ignoree (--no-db) : H2 en memoire'
elif docker_ready; then
  if [[ $DRY_RUN -eq 1 ]]; then
    info "docker compose -f $COMPOSE -p $PROJECT up -d db"
  else
    info 'Demarrage de PostgreSQL (docker compose, service db)'
    docker compose -f "$COMPOSE" -p "$PROJECT" up -d db
    ready=0
    for _ in $(seq 1 30); do
      if docker exec "$DB_CONTAINER" pg_isready -U wcpregnant >/dev/null 2>&1; then ready=1; break; fi
      sleep 2
    done
    if [[ $ready -eq 1 ]]; then ok 'PostgreSQL pret (localhost:5432, base wcpregnant)'; DB=postgres
    else warn 'PostgreSQL non pret : l API utilisera H2 en memoire'; DB=h2; fi
  fi
  export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wcpregnant
  export SPRING_DATASOURCE_USERNAME=wcpregnant
  export SPRING_DATASOURCE_PASSWORD=changeme
fi

# --- 3. Inférence agentique (100 % locale) ----------------------------------
export AGENT_INFERENCE_BASE_URL="$INFERENCE_URL"
export AGENT_MODEL="$INFERENCE_MODEL"
info "Inférence locale : $INFERENCE_URL (modele $INFERENCE_MODEL)"
if ! wait_http "$INFERENCE_URL/models" 2; then
  warn 'Serveur d inférence injoignable : /agent/chat repondra 503, le CRUD reste disponible'
fi

# --- 4. API Spring Boot -------------------------------------------------------
if [[ $MODE == docker ]]; then
  if [[ $DRY_RUN -eq 1 ]]; then
    info "docker compose -f $COMPOSE -p $PROJECT up -d"
  else
    port_free "$API_PORT" || warn "Port $API_PORT deja occupe : l API existante est utilisee"
    info 'Construction et demarrage de l API via docker-compose (build Maven)'
    export API_PORT="$API_PORT"
    docker compose -f "$COMPOSE" -p "$PROJECT" up -d
    if wait_port "$API_PORT" 180; then
      ok "API up : http://localhost:$API_PORT"
    else
      warn 'API non jointe dans les 180 s : voir docker logs wcpregnant-app'
    fi
  fi
elif port_free "$API_PORT"; then
  if [[ $DRY_RUN -eq 1 ]]; then
    info "mvn -f WcPregnantService/pom.xml spring-boot:run -Dspring-boot.run.profiles=$PROFILE -Dspring-boot.run.arguments=--server.port=$API_PORT"
  else
    info "Demarrage de l API Spring Boot sur le port $API_PORT (mvn spring-boot:run, profil $PROFILE)"
    mkdir -p "$RUN_DIR"
    mvn -f "$ROOT/WcPregnantService/pom.xml" spring-boot:run \
        -Dspring-boot.run.profiles="$PROFILE" \
        -Dspring-boot.run.arguments="--server.port=$API_PORT" \
        > "$RUN_DIR/backend.log" 2> "$RUN_DIR/backend.err" &
    API_PID=$!
    if wait_port "$API_PORT" 180; then
      ok "API up : http://localhost:$API_PORT (PID $API_PID)"
    else
      warn "API non jointe dans les 180 s : voir .run/backend.log"
    fi
  fi
else
  warn "Port $API_PORT deja occupe : l API est deja demarre, elle n est pas relancee"
fi

# --- 5. Frontend Angular ------------------------------------------------------
if [[ $NO_FRONT -eq 1 ]]; then
  warn 'Frontend ignore (--no-front)'
elif port_free "$FRONT_PORT"; then
  if [[ ! -d "$ROOT/WcPregnantApp/node_modules" ]]; then
    if [[ $DRY_RUN -eq 1 ]]; then info 'npm install (node_modules absent)'
    else info 'Installation des dependances npm'; npm install --prefix "$ROOT/WcPregnantApp"; fi
  fi
  if [[ $DRY_RUN -eq 1 ]]; then
    info "npm run serve -- --port $FRONT_PORT --proxy-config src/proxy.conf.json"
  else
    info "Demarrage du frontend Angular sur http://localhost:$FRONT_PORT"
    mkdir -p "$RUN_DIR"
    (cd "$ROOT/WcPregnantApp" && npm run serve -- --port "$FRONT_PORT" --proxy-config src/proxy.conf.json) \
        > "$RUN_DIR/frontend.log" 2> "$RUN_DIR/frontend.err" &
    FRONT_PID=$!
    if wait_port "$FRONT_PORT" 90; then
      ok "Frontend up : http://localhost:$FRONT_PORT (PID $FRONT_PID)"
    else
      warn 'Frontend non joint dans les 90 s : voir .run/frontend.log'
    fi
  fi
else
  warn "Port $FRONT_PORT deja occupe : le frontend est deja demarre"
fi

save_state

echo ''
echo '--------------------------------------------------------'
echo ' Solution demarre'
echo "   Frontend : http://localhost:$FRONT_PORT"
echo "   API      : http://localhost:$API_PORT  (Basic auth admin/admin, profil $PROFILE)"
echo "   Agent    : GET http://localhost:$API_PORT/agent/tools"
echo '   Arret    : ./stop.sh'
echo '--------------------------------------------------------'

