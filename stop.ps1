
#Requires -Version 5.1
<#
.SYNOPSIS
    Arret complet de la solution WcPregnant : frontend Angular, API Spring Boot, base PostgreSQL Docker.

.DESCRIPTION
    Arrete les processus enregistres par start.ps1 (.run/stack.state), puis tout processus encore
    en ecoute sur les ports de l API et du frontend, enfin les conteneurs docker-compose.
    -Status affiche l etat de la stack sans rien arreter.

.EXAMPLE
    .\stop.ps1
    .\stop.ps1 -PurgeVolumes
    .\stop.ps1 -Status
#>
[CmdletBinding()]
param(
    [int] $ApiPort = 14007,
    [int] $FrontPort = 4200,
    [switch] $PurgeVolumes,
    [switch] $Status
)

$Root           = Split-Path -Parent $MyInvocation.MyCommand.Path
$RunDir         = Join-Path $Root '.run'
$StateFile      = Join-Path $RunDir 'stack.state'
$Compose        = Join-Path $Root 'WcPregnantService\docker-compose.yml'
$ComposeProject = 'wcpregnant'

function Info($m) { Write-Host "  $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "  [OK] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  [!!] $m" -ForegroundColor Yellow }
function Have($c) { return [bool](Get-Command $c -ErrorAction SilentlyContinue) }

function DockerReady() {
    # Docker peut etre installe sans que le daemon soit joignable : on teste le daemon.
    if (-not (Have 'docker')) { return $false }
    try {
        docker info 2>&1 | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}

function Read-State {
    $s = @{}
    if (Test-Path $StateFile) {
        foreach ($line in Get-Content $StateFile) {
            if ($line -match '^(.+?)=(.*)$') { $s[$Matches[1]] = $Matches[2] }
        }
    }
    return $s
}

function Stop-ProcessById($procId, $label) {
    if (-not $procId) { return }
    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        Ok "Processus $label (PID $procId) arrete"
    } catch {
        Warn "Processus $label (PID $procId) deja termine ou inaccessible"
    }
}

function Stop-Port($port, $label) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { Info "Rien ne tourne sur le port $port ($label)" ; return }
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess | Sort-Object -Unique)) {
        Stop-ProcessById $procId $label
    }
}

Write-Host '==========================================================' -ForegroundColor Cyan
Write-Host ' WcPregnant - arret de la solution' -ForegroundColor Cyan
Write-Host '==========================================================' -ForegroundColor Cyan

$state = Read-State

if ($Status) {
    Info 'Etat de la stack'
    if (Test-Path $StateFile) { foreach ($line in Get-Content $StateFile) { Info "  $line" } }
    else { Warn 'Aucun .run\stack.state : la stack n a pas ete demarre par start.ps1' }
    foreach ($p in @($ApiPort, $FrontPort)) {
        $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
        if ($c) { Ok "Port $p en ecoute (PID $($c[0].OwningProcess))" } else { Info "Port $p libre" }
    }
    if (DockerReady) {
        try { docker ps --filter 'name=wcpregnant' --format '{{.Names}} | {{.Status}}' }
        catch { Warn 'Impossible d interroger Docker' }
    } else {
        Warn 'Docker indisponible : daemon non demarre'
    }
    exit 0
}

# 1. Processus lanca par start.ps1
if ($state['front_pid']) { Stop-ProcessById $state['front_pid'] 'frontend Angular' }
if ($state['api_pid'])   { Stop-ProcessById $state['api_pid'] 'API Spring Boot' }

# 2. Filet de securite : processus encore en ecoute (node, java enfants)
Stop-Port $FrontPort 'frontend Angular'
Stop-Port $ApiPort 'API Spring Boot'

# 3. Conteneurs Docker (PostgreSQL + eventuellement l API)
if (DockerReady) {
    try {
        if ($PurgeVolumes) {
            Info 'Arret des conteneurs et suppression du volume de donnees'
            docker compose -f $Compose -p $ComposeProject down -v
        } else {
            Info 'Arret des conteneurs (volume de donnees conserve)'
            docker compose -f $Compose -p $ComposeProject down
        }
        Ok 'Docker compose arrete'
    } catch {
        Warn "Echec de l arret docker-compose : $($_.Exception.Message)"
    }
} else {
    Warn 'Docker non disponible : rien a arreter du cote des conteneurs'
}

if (Test-Path $StateFile) { Remove-Item $StateFile -Force }

Write-Host ''
Ok 'Solution arretee'
Write-Host "  Logs de la session : .run\backend.log, .run\frontend.log"

exit 0

