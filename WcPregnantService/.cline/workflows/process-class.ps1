param(
    [Parameter(Mandatory=$true)]
    [string]$ClassName,
    
    [Parameter(Mandatory=$false)]
    [string]$ProjectDir = ""
)

# Déterminer le répertoire du projet par défaut si non fourni
if ([string]::IsNullOrEmpty($ProjectDir)) {
    $scriptPath = $MyInvocation.MyCommand.Definition
    if (-not [string]::IsNullOrWhiteSpace($scriptPath)) {
        $scriptDir = Split-Path -Parent $scriptPath
        if (-not [string]::IsNullOrWhiteSpace($scriptDir)) {
            $ProjectDir = Split-Path -Parent (Split-Path -Parent $scriptDir)
        }
    }

    if ([string]::IsNullOrEmpty($ProjectDir)) {
        $ProjectDir = (Get-Location).Path
    }
}

<#
.SYNOPSIS
    Traite une classe du projet : ajoute Javadoc et complète les TU pour >80% de couverture.
.DESCRIPTION
    Ce script est un assistant qui guide Cline dans le traitement d'une classe.
    Il liste les fichiers source et test, puis affiche les instructions à suivre.
.PARAMETER ClassName
    Nom de la classe à traiter (ex: ToiletteServiceImpl)
.PARAMETER ProjectDir
Répertoire racine du projet (par défaut : la racine détectée à partir du script)
.EXAMPLE
    .\process-class.ps1 -ClassName "ToiletteServiceImpl"
#>

# --- Configuration ---
$SourceBase = Join-Path $ProjectDir "src\main\java"
$TestBase = Join-Path $ProjectDir "src\test\java"
$PromptFile = Join-Path $ProjectDir ".cline\prompt\javadoc-tu.md"

# --- Vérification du prompt ---
if (-not (Test-Path $PromptFile)) {
    Write-Error "Prompt introuvable : $PromptFile"
    Write-Host "Assurez-vous que le fichier .cline/prompt/javadoc-tu.md existe" -ForegroundColor Yellow
    exit 1
}

# --- Recherche du fichier source ---
Write-Host "`n=== Recherche de la classe $ClassName ===" -ForegroundColor Cyan

$SourceFile = Get-ChildItem -Path $SourceBase -Recurse -Filter "$ClassName.java" | Select-Object -First 1
if (-not $SourceFile) {
    Write-Error "Fichier source introuvable pour $ClassName"
    exit 1
}
Write-Host "  Source : $($SourceFile.FullName)" -ForegroundColor Green

# --- Recherche du fichier de test ---
$TestFile = Get-ChildItem -Path $TestBase -Recurse -Filter "*$ClassName*Test*.java" | Select-Object -First 1
if (-not $TestFile) {
    Write-Host "  Test : AUCUN (à créer)" -ForegroundColor Yellow
} else {
    Write-Host "  Test  : $($TestFile.FullName)" -ForegroundColor Green
}

# --- Extraction des méthodes publiques ---
Write-Host "`n=== Méthodes publiques détectées ===" -ForegroundColor Cyan
$Content = Get-Content $SourceFile.FullName -Raw

# Pattern pour extraire les signatures de méthodes publiques (avec ou sans @Override)
$MethodPattern = '(?s)(?:public\s+\w+\s+(\w+)\s*\([^)]*\)\s*\{)'
$Methods = [regex]::Matches($Content, $MethodPattern)

$MethodList = @()
$JavadocMissingList = @()
foreach ($m in $Methods) {
    $methodName = $m.Groups[1].Value
    # Exclure les méthodes héritées (equals, hashCode, toString)
    if ($methodName -notin @('equals', 'hashCode', 'toString', 'getClass', 'notify', 'notifyAll', 'wait')) {
        $MethodList += $methodName
        
        # Vérifier la présence de Javadoc avant la méthode
        $offset = $m.Index
        $preceding = $Content.Substring(0, $offset)
        $lastComment = $preceding.LastIndexOf('/**')
        $hasJavadoc = $false
        if ($lastComment -ge 0) {
            $comment = $Content.Substring($lastComment, $offset - $lastComment)
            if ($comment -match '^\s*/\*\*') { $hasJavadoc = $true }
        }
        
        if (-not $hasJavadoc) {
            $JavadocMissingList += $methodName
            Write-Host "  [ ] $methodName (Javadoc manquant)" -ForegroundColor Red
        } else {
            Write-Host "  [x] $methodName" -ForegroundColor Green
        }
    }
}

if ($MethodList.Count -eq 0) {
    Write-Warning "Aucune méthode publique trouvée dans $ClassName"
}

# --- Analyse des TU existants ---
$TestCount = 0
$TestMethods = @()
if ($TestFile) {
    $TestContent = Get-Content $TestFile.FullName -Raw
    $TestMatches = [regex]::Matches($TestContent, '@Test')
    $TestCount = $TestMatches.Count
    $TestMethodMatches = [regex]::Matches($TestContent, '(?:void\s+(\w+)\s*\()')
    $TestMethods = $TestMethodMatches | ForEach-Object { $_.Groups[1].Value }
}

Write-Host "`n=== Analyse des tests ===" -ForegroundColor Cyan
if ($TestFile) {
    Write-Host "  Fichier de test : $($TestFile.FullName)" -ForegroundColor Gray
    Write-Host "  Nombre de tests : $TestCount" -ForegroundColor $(if ($TestCount -ge ($MethodList.Count * 2)) { "Green" } else { "Yellow" })
    foreach ($tm in $TestMethods) {
        Write-Host "    - $tm" -ForegroundColor Gray
    }
} else {
    Write-Host "  Aucun fichier de test trouvé" -ForegroundColor Yellow
}

# --- Instructions pour Cline ---
Write-Host "`n=== INSTRUCTIONS POUR CLINE ===" -ForegroundColor Magenta
Write-Host "Classe : $ClassName" -ForegroundColor Yellow
Write-Host "Source : $($SourceFile.FullName)" -ForegroundColor Yellow
if ($TestFile) {
    Write-Host "Test   : $($TestFile.FullName)" -ForegroundColor Yellow
} else {
    Write-Host "Test   : À créer" -ForegroundColor Yellow
}

Write-Host "`nTâches à effectuer :" -ForegroundColor Cyan

if ($JavadocMissingList.Count -gt 0) {
    Write-Host "`n  JAVADOC À AJOUTER ($($JavadocMissingList.Count)) :" -ForegroundColor Red
    foreach ($jm in $JavadocMissingList) {
        Write-Host "    - $jm" -ForegroundColor White
    }
}

$missingMethods = $MethodList | Where-Object { 
    $found = $false
    foreach ($tm in $TestMethods) { if ($tm -match $_) { $found = $true } }
    -not $found
}
if ($missingMethods.Count -gt 0) {
    Write-Host "`n  TU MANQUANTS ($($missingMethods.Count)) :" -ForegroundColor Red
    foreach ($mm in $missingMethods) {
        Write-Host "    - $mm" -ForegroundColor White
    }
}

Write-Host "`nÉtapes :" -ForegroundColor Cyan
Write-Host "  1. Lire le fichier source : read_file <path>$($SourceFile.FullName.Replace($ProjectDir, '').TrimStart('\'))</path>" -ForegroundColor Gray
if ($TestFile) {
    Write-Host "  2. Lire le fichier de test : read_file <path>$($TestFile.FullName.Replace($ProjectDir, '').TrimStart('\'))</path>" -ForegroundColor Gray
}
Write-Host "  3. Pour chaque méthode, appliquer le prompt .cline/prompt/javadoc-tu.md" -ForegroundColor Gray
Write-Host "  4. Valider : mvn test -Dtest=$($TestFile.BaseName) -Dsurefire.useFile=false" -ForegroundColor Gray

Write-Host "`n=== FIN ===" -ForegroundColor Cyan