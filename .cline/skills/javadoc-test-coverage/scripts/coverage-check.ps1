param(
    [string]$Report = "WcPregnantService/target/site/jacoco/jacoco.csv",
    [double]$Threshold = 80
)

if (-not (Test-Path $Report)) {
    Write-Host "Rapport JaCoCo introuvable : $Report"
    Write-Host "Lancez d'abord : mvn test jacoco:report"
    exit 1
}

$rows = Import-Csv -Path $Report
$below = @()
$totalCovered = 0
$totalMissed = 0

foreach ($r in $rows) {
    $covered = [int]$r.LINE_COVERED
    $missed  = [int]$r.LINE_MISSED
    $total   = $covered + $missed
    $totalCovered += $covered
    $totalMissed  += $missed
    if ($total -eq 0) { continue }
    $pct = [math]::Round(100.0 * $covered / $total, 1)
    if ($pct -lt $Threshold) {
        $below += [pscustomobject]@{ Class = $r.CLASS; Covered = $covered; Missed = $missed; Percent = $pct }
    }
}

$grandTotal = $totalCovered + $totalMissed
$globalPct = if ($grandTotal -gt 0) { [math]::Round(100.0 * $totalCovered / $grandTotal, 1) } else { 0 }

Write-Host "Couverture globale : $globalPct %  ($totalCovered / $grandTotal lignes)"
if ($below.Count -eq 0) {
    Write-Host "Toutes les classes sont >= $Threshold %."
    exit 0
}

Write-Host "`Classes sous le seuil de $Threshold % :"
$below | Sort-Object Percent | Format-Table -AutoSize
Write-Host "INFO: $($below.Count) classe(s) restent sous $Threshold %."
exit 0
