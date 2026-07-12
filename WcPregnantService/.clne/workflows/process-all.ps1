param(
    [Parameter(Mandatory=$false)]
    [string]$ProjectDir = ""
)

# Déterminer le répertoire du projet par défaut si non fourni (script placé dans .clne\workflows)
if ([string]::IsNullOrEmpty($ProjectDir)) {
    # remonter de 3 niveaux : workflows -> .clne -> projet
    $ProjectDir = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Definition))
}
# Diagnostic: afficher ProjectDir et son type pour debugging
Write-Host "DEBUG: ProjectDir='$ProjectDir' (Type=$($ProjectDir.GetType().FullName))" -ForegroundColor Cyan

<#
.SYNOPSIS
    Traite toutes les classes du projet : ajoute Javadoc et complète les TU pour >80% de couverture.
.DESCRIPTION
    Ce script liste toutes les classes du projet et génère les commandes Cline
    pour traiter chaque classe une par une.
.PARAMETER ProjectDir
    Répertoire racine du projet (par défaut : répertoire courant)
.EXAMPLE
    .\process-all.ps1
#>

# --- Configuration ---
$SourceBase = Join-Path $ProjectDir "src\main\java"
$TestBase = Join-Path $ProjectDir "src\test\java"
# Localisation possible du prompt (support .clne/.cline, prompt/prompts)
$PromptCandidates = @(
    "$ProjectDir\.clne\prompts\javadoc-tu.md",
    "$ProjectDir\.cline\prompt\javadoc-tu.md",
    "$ProjectDir\.clne\prompt\javadoc-tu.md",
    "$ProjectDir\.cline\prompts\javadoc-tu.md"
)
$PromptFile = $PromptCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

# --- Vérification du prompt ---
if (-not $PromptFile) {
    Write-Error "Prompt introuvable dans : $($PromptCandidates -join '; ')"
    exit 1
}

# --- Recherche de toutes les classes Java (sauf les classes de configuration/exceptions/DTO) ---
Write-Host "`n=== Recherche des classes à traiter ===" -ForegroundColor Cyan

$ExcludePatterns = @(
    '*Application*', '*Config*', '*Exception*', '*DTO*', '*BO*', '*Mapper*',
    '*Repository*', '*Controller*', '*Resource*', '*Request*', '*Response*'
)

$AllClasses = Get-ChildItem -Path $SourceBase -Recurse -Filter "*.java" | Where-Object {
    $include = $true
    foreach ($pattern in $ExcludePatterns) {
        if ($_.Name -like $pattern) { $include = $false; break }
    }
    # Exclure les interfaces (on ne traite que les implémentations)
    $content = Get-Content $_.FullName -Raw
    if ($content -match '\binterface\b') { $include = $false }
    return $include
}

if ($AllClasses.Count -eq 0) {
    Write-Warning "Aucune classe à traiter trouvée"
    exit 0
}

Write-Host "Classes à traiter : $($AllClasses.Count)" -ForegroundColor Green

# --- Analyse de chaque classe ---
$ClassReport = @()
foreach ($class in $AllClasses) {
    $className = $class.BaseName
    $relativePath = $class.FullName.Replace($ProjectDir, '').TrimStart('\')
    
    # Recherche du test correspondant
    $testFile = Get-ChildItem -Path $TestBase -Recurse -Filter "*$className*Test*.java" | Select-Object -First 1
    
    # Extraction des méthodes publiques
    $content = Get-Content $class.FullName -Raw
    $MethodPattern = '(?s)(?:public\s+\w+\s+(\w+)\s*\([^)]*\)\s*\{)'
    $methods = [regex]::Matches($content, $MethodPattern)
    
    $methodNames = @()
    foreach ($m in $methods) {
        $methodName = $m.Groups[1].Value
        if ($methodName -notin @('equals', 'hashCode', 'toString', 'getClass', 'notify', 'notifyAll', 'wait')) {
            $methodNames += $methodName
        }
    }
    
    $hasTest = $testFile -ne $null
    
    $ClassReport += [PSCustomObject]@{
        ClassName = $className
        SourcePath = $relativePath
        HasTest = $hasTest
        TestPath = if ($hasTest) { $testFile.FullName.Replace($ProjectDir, '').TrimStart('\') } else { "À créer" }
        MethodCount = $methodNames.Count
        Methods = $methodNames -join ', '
    }
}

# --- Affichage du rapport ---
Write-Host "`n=== RAPPORT DES CLASSES À TRAITER ===" -ForegroundColor Magenta
Write-Host "`nLegende : [OK] Test existant | [ ] Test a creer" -ForegroundColor Gray
Write-Host ("=" * 80) -ForegroundColor DarkGray

foreach ($report in $ClassReport) {
    $testStatus = if ($report.HasTest) { "[OK]" } else { "[ ]" }
    Write-Host "`n$testStatus $($report.ClassName) ($($report.MethodCount) méthodes)" -ForegroundColor Yellow
    Write-Host "  Source : $($report.SourcePath)" -ForegroundColor Gray
    Write-Host "  Test   : $($report.TestPath)" -ForegroundColor Gray
    Write-Host "  Méthodes : $($report.Methods)" -ForegroundColor White
}

# --- Génération des commandes Cline ---
Write-Host "`n=== COMMANDES CLINE À EXÉCUTER ===" -ForegroundColor Cyan
Write-Host "Exécuter ces commandes une par une dans l'ordre :" -ForegroundColor White
Write-Host ("=" * 80) -ForegroundColor DarkGray

$index = 1
foreach ($report in $ClassReport) {
    Write-Host "`n[$index/$($ClassReport.Count)] $($report.ClassName)" -ForegroundColor Green
    Write-Host "  1. Lire la source : read_file <path>$($report.SourcePath)</path>" -ForegroundColor Gray
    if ($report.HasTest) {
        Write-Host "  2. Lire le test   : read_file <path>$($report.TestPath)</path>" -ForegroundColor Gray
    }
    Write-Host "  3. Appliquer le prompt .clne/prompts/javadoc-tu.md" -ForegroundColor Gray
    Write-Host "  4. Valider        : mvn test -Dtest=$($report.ClassName)Test -Dsurefire.useFile=false" -ForegroundColor Gray
    $index++
}

# --- Résumé ---
Write-Host "`n=== RÉSUMÉ ===" -ForegroundColor Magenta
$totalMethods = ($ClassReport | Measure-Object -Property MethodCount -Sum).Sum
$classesWithTest = ($ClassReport | Where-Object { $_.HasTest }).Count
Write-Host "Classes à traiter : $($ClassReport.Count)" -ForegroundColor White
Write-Host "Méthodes à couvrir : $totalMethods" -ForegroundColor White
Write-Host "Classes avec TU    : $classesWithTest/$($ClassReport.Count)" -ForegroundColor White
Write-Host "Classes sans TU    : $($ClassReport.Count - $classesWithTest)/$($ClassReport.Count)" -ForegroundColor $(if ($classesWithTest -eq $ClassReport.Count) { "Green" } else { "Yellow" })
Write-Host "`nPrompt utilisé : $PromptFile" -ForegroundColor Gray
Write-Host "`n=== FIN ===" -ForegroundColor Cyan