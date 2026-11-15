#!/usr/bin/env bash
# Arret complet de la solution WcPregnant : frontend Angular, API Spring Boot, conteneurs Docker.
#
#   ./stop.sh                    # arrete tout
#   ./stop.sh --purge-volumes    # supprime aussi le volume PostgreSQL
#   ./stop.sh --status           # afficher l etat sans rien arreter
set -euo pipefail

API_PORT=8081
FRONT_PORT=4200
PURGE=0
STATUS=0

usage() {
  cat <<'EOF'
Usage : ./stop.sh [options]
  --api-port PORT     port de l API (defaut 8081)
  --front-port PORT   port du frontend (defaut 4200)
  --purge-volumes     supprime le volume de donnees PostgreSQL
  --status            afficher l etat de la stack sans arreter
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --api-port) API_PORT="$2"; shift 2 ;;
    --front-port) FRONT_PORT="$2"; shift 2 ;;
    --purge-volumes) PURGE=1; shift ;;
    --status) STATUS=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Option inconnue : $1" >&2; usage; exit 1 ;;
  esac
done

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.run"
STATE="$RUN_DIR/stack.state"
COMPOSE="$ROOT/WcPregnantService/docker-compose.yml"
PROJECT="wcpregnant"

info() { echo "  $1"; }
ok()   { echo "  [OK] $1"; }
warn() { echo "  [!!] $1"; }

docker_ready() {
  command -v docker >/dev/null && docker info >/dev/null 2>&1
}

stop_pid() {
  pid="$1"; label="$2"
  if [[ -z "$pid" ]]; then return 0; fi
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    sleep 2
    kill -9 "$pid" 2>/dev/null || true
    ok "Processus $label (PID $pid) arrete"
  else
    warn "Processus $label (PID $pid) deja termine"
  fi
}

echo '=========================================================='
echo ' WcPregnant - arret de la solution'
echo '=========================================================='

API_PID=""
FRONT_PID=""
if [[ -f "$STATE" ]]; then
  while IFS='=' read -r key value; do
    if [[ "$key" == api_pid ]]; then API_PID="$value"; fi
    if [[ "$key" == front_pid ]]; then FRONT_PID="$value"; fi
  done < "$STATE"
fi

if [[ $STATUS -eq 1 ]]; then
  if [[ -f "$STATE" ]]; then cat "$STATE"; else warn 'Aucun .run/stack.state : stack non demarre par start.sh'; fi
  info "Ports surveilles : API $API_PORT, frontend $FRONT_PORT"
  if docker_ready; then docker ps --filter 'name=wcpregnant' --format '{{.Names}} | {{.Status}}'
  else warn 'Docker injoignable : daemon non demarre'; fi
  exit 0
fi

stop_pid "$FRONT_PID" 'frontend Angular'
stop_pid "$API_PID" 'API Spring Boot'

# Filet de securite : enfants java / node encore actifs
pkill -f "server.port=$API_PORT" 2>/dev/null || true
pkill -f "proxy.conf.json" 2>/dev/null || true
ok 'Processus residuels eventuels stops'

if docker_ready; then
  if [[ $PURGE -eq 1 ]]; then
    info 'Arret des conteneurs et suppression du volume de donnees'
    docker compose -f "$COMPOSE" -p "$PROJECT" down -v || warn 'docker compose down echoue (daemon indisponible ?)'
  else
    info 'Arret des conteneurs (volume de donnees conserve)'
    docker compose -f "$COMPOSE" -p "$PROJECT" down || warn 'docker compose down echoue (daemon indisponible ?)'
  fi
  ok 'Docker compose arrete'
else
  warn 'Docker non disponible : rien a arreter du cote des conteneurs'
fi

if [[ -f "$STATE" ]]; then rm -f "$STATE"; fi

echo ''
ok 'Solution arretee'
echo '  Logs de la session : .run/backend.log, .run/frontend.log'
