param(
    [Parameter(Mandatory=$false)]
    [string]$ProjectDir = ""
)

# Déterminer le répertoire du projet par défaut si non fourni
if ([string]::IsNullOrEmpty($ProjectDir)) {
    $ProjectDir = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Definition))
}

<#
.SYNOPSIS
    Traite toutes les classes du projet : ajoute Javadoc et complète les TU pour >80% de couverture.
.DESCRIPTION
    Ce script liste toutes les classes du projet, vérifie les Javadoc et les TU,
    et génère les commandes Cline pour traiter chaque classe manquante.
.PARAMETER ProjectDir
    Répertoire racine du projet (par défaut : répertoire courant)
.EXAMPLE
    .\process-all.ps1
#>

# --- Configuration ---
$SourceBase = Join-Path $ProjectDir "src\main\java"
$TestBase = Join-Path $ProjectDir "src\test\java"

# --- Recherche de toutes les classes Java (sauf classes techniques) ---
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
    # Exclure les interfaces
    $content = Get-Content $_.FullName -Raw
    if ($content -match '\binterface\s') { $include = $false }
    return $include
}

if ($AllClasses.Count -eq 0) {
    Write-Warning "Aucune classe à traiter trouvée"
    exit 0
}

Write-Host "Classes à traiter : $($AllClasses.Count)" -ForegroundColor Green

# --- Analyse de chaque classe ---
$ClassReport = @()
$HasMissingJavadoc = $false
$HasMissingTests = $false

foreach ($class in $AllClasses) {
    $className = $class.BaseName
    $relativePath = $class.FullName.Replace($ProjectDir, '').TrimStart('\')
    
    # Recherche du test correspondant
    $testFile = Get-ChildItem -Path $TestBase -Recurse -Filter "*$className*Test*.java" | Select-Object -First 1
    
    # Analyse du contenu source
    $content = Get-Content $class.FullName -Raw
    
    # Extraction des méthodes publiques (hors méthodes héritées)
    $MethodPattern = '(?s)(?:public\s+(?:\w+\s+)?(\w+)\s*\([^)]*\)\s*(?:\{|;))'
    $methods = [regex]::Matches($content, $MethodPattern)
    
    $methodReport = @()
    foreach ($m in $methods) {
        $methodName = $m.Groups[1].Value
        if ($methodName -notin @('equals', 'hashCode', 'toString', 'getClass', 'notify', 'notifyAll', 'wait')) {
            # Vérifier si un Javadoc existe AVANT la méthode
            $methodOffset = $m.Index
            $precedingText = $content.Substring(0, $methodOffset)
            $lastCommentStart = $precedingText.LastIndexOf('/**')
            if ($lastCommentStart -ge 0) {
                $commentText = $content.Substring($lastCommentStart, $methodOffset - $lastCommentStart)
                $hasJavadoc = $commentText -match '^\s*/\*\*'
            } else {
                $hasJavadoc = $false
            }
            
            if (-not $hasJavadoc) {
                $HasMissingJavadoc = $true
            }
            
            $methodReport += [PSCustomObject]@{
                MethodName = $methodName
                HasJavadoc = $hasJavadoc
            }
        }
    }
    
    $hasTest = $testFile -ne $null
    if (-not $hasTest) {
        $HasMissingTests = $true
    }
    
    # Vérifier le nombre de TU dans le fichier de test
    $testCount = 0
    $namedTests = ""
    if ($hasTest) {
        $testContent = Get-Content $testFile.FullName -Raw
        $testMatches = [regex]::Matches($testContent, '@Test')
        $testCount = $testMatches.Count
        $testMethodMatches = [regex]::Matches($testContent, '(?:void\s+(\w+)\s*\()')
        $namedTests = ($testMethodMatches | ForEach-Object { $_.Groups[1].Value }) -join ', '
    }
    
    $ClassReport += [PSCustomObject]@{
        ClassName = $className
        SourcePath = $relativePath
        HasTest = $hasTest
        TestCount = $testCount
        TestPath = if ($hasTest) { $testFile.FullName.Replace($ProjectDir, '').TrimStart('\') } else { "À créer" }
        NamedTests = $namedTests
        MethodCount = $methodReport.Count
        MethodsWithJavadoc = ($methodReport | Where-Object { $_.HasJavadoc }).Count
        Methods = ($methodReport | ForEach-Object { $_.MethodName }) -join ', '
        MissingJavadocCount = ($methodReport | Where-Object { -not $_.HasJavadoc }).Count
    }
}

# --- Affichage du rapport détaillé ---
Write-Host "`n=== RAPPORT DÉTAILLÉ ===" -ForegroundColor Magenta
Write-Host ("=" * 100) -ForegroundColor DarkGray

foreach ($report in $ClassReport) {
    $testIcon = if ($report.HasTest) { "[OK]" } else { "[  ]" }
    $javadocIcon = if ($report.MissingJavadocCount -eq 0) { "[OK]" } else { "[  ]" }
    
    Write-Host "`n$($report.ClassName)" -ForegroundColor Yellow
    Write-Host "  Source     : $($report.SourcePath)" -ForegroundColor Gray
    Write-Host "  Javadoc    : $javadocIcon ($($report.MethodsWithJavadoc)/$($report.MethodCount) méthodes documentées)" -ForegroundColor $(if ($report.MissingJavadocCount -eq 0) { "Green" } else { "Red" })
    Write-Host "  Tests      : $testIcon ($($report.TestCount) méthodes testées)" -ForegroundColor $(if ($report.HasTest) { "Green" } else { "Red" })
    Write-Host "  Tests path : $($report.TestPath)" -ForegroundColor Gray
    if ($report.TestCount -gt 0) {
        Write-Host "  Tests list : $($report.NamedTests)" -ForegroundColor Gray
    }
    Write-Host "  Méthodes   : $($report.Methods)" -ForegroundColor White
}

# --- Résumé ---
Write-Host "`n=== RÉSUMÉ ===" -ForegroundColor Magenta
$totalMethods = ($ClassReport | Measure-Object -Property MethodCount -Sum).Sum
$classesWithTest = ($ClassReport | Where-Object { $_.HasTest }).Count
$totalMissingJavadoc = ($ClassReport | Measure-Object -Property MissingJavadocCount -Sum).Sum

Write-Host "Classes à traiter     : $($ClassReport.Count)" -ForegroundColor White
Write-Host "Méthodes totales       : $totalMethods" -ForegroundColor White
Write-Host "Javadoc manquants      : $totalMissingJavadoc" -ForegroundColor $(if ($totalMissingJavadoc -eq 0) { "Green" } else { "Red" })
Write-Host "Classes avec TU        : $classesWithTest/$($ClassReport.Count)" -ForegroundColor White
Write-Host "Classes sans TU        : $($ClassReport.Count - $classesWithTest)/$($ClassReport.Count)" -ForegroundColor $(if ($classesWithTest -eq $ClassReport.Count) { "Green" } else { "Yellow" })

# --- Génération des commandes Cline ---
Write-Host "`n=== COMMANDES À EXÉCUTER ===" -ForegroundColor Cyan
Write-Host "Exécuter ces commandes dans l'ordre :" -ForegroundColor White
Write-Host ("=" * 100) -ForegroundColor DarkGray

$index = 1
foreach ($report in $ClassReport) {
    Write-Host "`n[$index] $($report.ClassName)" -ForegroundColor Green
    if ($report.MissingJavadocCount -gt 0 -or -not $report.HasTest) {
        Write-Host "  read_file <path>$($report.SourcePath)</path>" -ForegroundColor Gray
        if ($report.HasTest) {
            Write-Host "  read_file <path>$($report.TestPath)</path>" -ForegroundColor Gray
        }
        Write-Host "  Appliquer le prompt javadoc-tu.md :" -ForegroundColor Gray
        Write-Host "    - Ajouter Javadoc sur $($report.MissingJavadocCount) méthode(s)" -ForegroundColor Gray
        Write-Host "    - $(if ($report.HasTest) { 'Compléter' } else { 'Créer' }) les TU" -ForegroundColor Gray
        Write-Host "  mvn test -Dtest=$($report.ClassName)Test" -ForegroundColor Gray
    } else {
        Write-Host "  ✅ Déjà complet (Javadoc + TU présents)" -ForegroundColor Green
    }
    $index++
}

Write-Host "`n=== FIN ===" -ForegroundColor Cyan