#Requires -Version 5.1
<#
.SYNOPSIS
    Demarrage complet de la solution WcPregnant : base PostgreSQL (Docker), API Spring Boot, frontend Angular.

.DESCRIPTION
    Mode "local" (defaut)  : PostgreSQL via docker-compose, API lancee par Maven, frontend via ng serve.
    Mode "docker"         : base + API via docker-compose (image construite depuis le Dockerfile).
    Les PID des processus lance sont enregistres dans .run/stack.state ; stop.ps1 les arrete.
    Sans Docker, l'API bascule sur H2 en memoire : la solution reste utilisable.

.EXAMPLE
    .\start.ps1
    .\start.ps1 -Mode docker -NoFront
    .\start.ps1 -ApiPort 14007 -FrontPort 4200
    .\start.ps1 -DryRun
#>
[CmdletBinding()]
param(
    [ValidateSet('local', 'docker')] [string] $Mode = 'local',
    [string] $Profile = 'dev',
    [int] $ApiPort = 14007,
    [int] $FrontPort = 4200,
    [string] $InferenceUrl = 'http://localhost:8000/v1',
    [string] $InferenceModel = 'Qwen-3.8-Flash-Next',
    [switch] $NoDb,
    [switch] $NoFront,
    [switch] $DryRun
)

$Root           = Split-Path -Parent $MyInvocation.MyCommand.Path
$RunDir         = Join-Path $Root '.run'
$StateFile      = Join-Path $RunDir 'stack.state'
$Compose        = Join-Path $Root 'WcPregnantService\docker-compose.yml'
$ComposeProject = 'wcpregnant'
$DbContainer    = 'wcpregnant-db'
$state          = [ordered]@{ mode = $Mode; api_port = $ApiPort; front_port = $FrontPort; db = 'none' }

function Info($m) { Write-Host "  $m" -ForegroundColor Cyan }
function Ok($m)   { Write-Host "  [OK] $m" -ForegroundColor Green }
function Warn($m) { Write-Host "  [!!] $m" -ForegroundColor Yellow }
function Fail($m) { Write-Host "  [XX] $m" -ForegroundColor Red }
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

function PortFree($p) {
    $c = Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue
    return ($c -eq $null)
}

function PortUp($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port($port, $seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        if (PortUp $port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Wait-Http($url, $seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        try {
            $r = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Save-State {
    if ($DryRun) { return }
    $lines = foreach ($k in $state.Keys) { "$($k)=$($state[$k])" }
    $lines | Set-Content -Path $StateFile
}

Write-Host '==========================================================' -ForegroundColor Cyan
Write-Host " WcPregnant - demarrage de la solution (mode $Mode)" -ForegroundColor Cyan
Write-Host '==========================================================' -ForegroundColor Cyan
if ($DryRun) { Warn 'Mode sec (-DryRun) : aucune commande ne sera lancee' }

# --- 1. Preconditions ---------------------------------------------------------
Info 'Verification des pre requis'
$missing = @()
if ($Mode -eq 'local' -and -not (Have 'mvn')) { $missing += 'Maven (mvn)' }
if (-not (Have 'npm')) { $missing += 'Node.js / npm' }
if ($missing.Count -gt 0) { Fail "Pre requis manquant : $($missing -join ', ')"; exit 1 }
$dockerUp = DockerReady
if (-not $dockerUp) { Warn 'Docker indisponible (daemon non demarre) : PostgreSQL ne sera pas demarre, Spring bascule sur H2 en memoire' }
Ok 'Pre requis OK'

if (-not (Test-Path $RunDir)) { New-Item -ItemType Directory -Path $RunDir | Out-Null }

# --- 2. Base de donnees PostgreSQL ------------------------------------------
if ($NoDb) {
    Warn 'Base de donnees ignoree (-NoDb) : H2 en memoire'
} elseif ($dockerUp) {
    if ($DryRun) {
        Info "docker compose -f $Compose -p $ComposeProject up -d db"
    } else {
        Info 'Demarrage de PostgreSQL (docker compose, service db)'
        try {
            docker compose -f $Compose -p $ComposeProject up -d db
            $ready = $false
            for ($i = 0; $i -lt 30; $i++) {
                docker exec $DbContainer pg_isready -U wcpregnant 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) { $ready = $true; break }
                Start-Sleep -Seconds 2
            }
            if ($ready) { Ok 'PostgreSQL pret (localhost:5432, base wcpregnant)' }
            else { Warn 'PostgreSQL non pret : l API utilisera H2 en memoire' }
            if ($ready) { $state['db'] = 'postgres' } else { $state['db'] = 'h2' }
        } catch {
            Warn "Echec du demarrage de PostgreSQL : $($_.Exception.Message) - l API utilisera H2 en memoire"
            $state['db'] = 'h2'
        }
    }
    $env:SPRING_DATASOURCE_URL      = 'jdbc:postgresql://localhost:5432/wcpregnant'
    $env:SPRING_DATASOURCE_USERNAME = 'wcpregnant'
    $env:SPRING_DATASOURCE_PASSWORD = 'changeme'
}

# --- 3. Configuration de l inference agentique (100 % locale) ---------------
$env:AGENT_INFERENCE_BASE_URL = $InferenceUrl
$env:AGENT_MODEL              = $InferenceModel
Info "Inference locale : $InferenceUrl (modele $InferenceModel)"
if (-not (Wait-Http "$InferenceUrl/models" 2)) {
    Warn 'Serveur d inference injoignable : /agent/chat repondra 503, le CRUD reste disponible'
}

# --- 4. API Spring Boot -------------------------------------------------------
if ($Mode -eq 'docker') {
    if (-not $dockerUp) { Fail 'Mode docker exige un daemon Docker operationnel'; exit 1 }
    if ($DryRun) {
        Info "docker compose -f $Compose -p $ComposeProject up -d"
    } else {
        if (-not (PortFree $ApiPort)) { Warn "Port $ApiPort deja occupe : l API existante est utilisee" }
        Info 'Construction et demarrage de l API via docker-compose (build Maven)'
        $env:API_PORT = "$ApiPort"
        try {
            docker compose -f $Compose -p $ComposeProject up -d
            if (Wait-Port $ApiPort 180) { Ok "API up : http://localhost:$ApiPort" }
            else { Warn 'API non jointe dans les 180 s : voir docker logs wcpregnant-app' }
            $state['api'] = 'docker'
        } catch {
            Warn "Echec du demarrage docker-compose : $($_.Exception.Message)"
        }
    }
} elseif (PortFree $ApiPort) {
    if ($DryRun) {
        Info "mvn -f WcPregnantService\pom.xml spring-boot:run -Dspring-boot.run.profiles=$Profile -Dspring-boot.run.arguments=--server.port=$ApiPort"
    } else {
        Info "Demarrage de l API Spring Boot sur le port $ApiPort (mvn spring-boot:run, profil $Profile)"
        try {
            $p = Start-Process -FilePath 'mvn' `
                -ArgumentList '-f', (Join-Path $Root 'WcPregnantService\pom.xml'), 'spring-boot:run', `
                              "-Dspring-boot.run.profiles=$Profile", `
                              "-Dspring-boot.run.arguments=--server.port=$ApiPort" `
                -RedirectStandardOutput (Join-Path $RunDir 'backend.log') `
                -RedirectStandardError  (Join-Path $RunDir 'backend.err') `
                -PassThru -WindowStyle Hidden
            $state['api_pid'] = $p.Id
        } catch {
            Warn "Echec du demarrage de l API : $($_.Exception.Message)"
        }
        if (Wait-Port $ApiPort 180) { Ok "API up : http://localhost:$ApiPort" }
        else { Warn "API non jointe dans les 180 s : voir .run\backend.log" }
    }
} else {
    Warn "Port $ApiPort deja occupe : l API est deja demarre, elle n est pas relancee"
}

# --- 5. Frontend Angular ------------------------------------------------------
if ($NoFront) {
    Warn 'Frontend ignore (-NoFront)'
} elseif (PortFree $FrontPort) {
    $appDir = Join-Path $Root 'WcPregnantApp'
    if (-not (Test-Path (Join-Path $appDir 'node_modules'))) {
        if ($DryRun) { Info 'npm install (node_modules absent)' }
        else { Info 'Installation des dependances npm'; npm install --prefix $appDir }
    }
    if ($DryRun) {
        Info "npm run serve -- --port $FrontPort --proxy-config src/proxy.conf.json"
    } else {
        Info "Demarrage du frontend Angular sur http://localhost:$FrontPort"
        try {
            # npm est un script PowerShell (npm.ps1) : il doit etre lance via cmd.exe
            $p = Start-Process -FilePath 'cmd.exe' `
                -ArgumentList '/c', "npm run serve -- --port $FrontPort --proxy-config src/proxy.conf.json" `
                -WorkingDirectory $appDir `
                -RedirectStandardOutput (Join-Path $RunDir 'frontend.log') `
                -RedirectStandardError  (Join-Path $RunDir 'frontend.err') `
                -PassThru -WindowStyle Hidden
            $state['front_pid'] = $p.Id
        } catch {
            Warn "Echec du demarrage du frontend : $($_.Exception.Message)"
        }
        if (Wait-Port $FrontPort 90) { Ok "Frontend up : http://localhost:$FrontPort" }
        else { Warn 'Frontend non joint dans les 90 s : voir .run\frontend.log' }
    }
} else {
    Warn "Port $FrontPort deja occupe : le frontend est deja demarre, il n est pas relance"
}

Save-State

Write-Host ''
Write-Host '--------------------------------------------------------' -ForegroundColor Cyan
Write-Host ' Solution demarre' -ForegroundColor Green
Write-Host "   Frontend : http://localhost:$FrontPort"
Write-Host "   API      : http://localhost:$ApiPort  (Basic auth admin/admin, profil $Profile)"
Write-Host "   Agent    : GET http://localhost:$ApiPort/agent/tools"
Write-Host "   Arret    : .\stop.ps1"
Write-Host '--------------------------------------------------------' -ForegroundColor Cyan

exit 0


