$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8087'

Write-Host "=== START VERIFICATION PHASE 8: ADMIN REPORTS & CASH FLOW (PORT 8087) ==="

# 1. Healthcheck
Write-Host "1. Checking server healthcheck..."
$health = Invoke-WebRequest "$base/login" -UseBasicParsing
if ($health.StatusCode -ne 200) {
    throw "Server did not respond with 200 at /login (actual: $($health.StatusCode))"
}
Write-Host "-> Healthcheck OK."

# 2. Authorization check /api/admin/reports (Cases 60, 63)
Write-Host "2. Checking authorization on /api/admin/reports..."

# 2.1 Anonymous access
$anonBlocked = $false
try {
    $res = Invoke-WebRequest "$base/api/admin/reports" -UseBasicParsing
} catch {
    if ($_.Exception.Response.StatusCode.value__ -in @(401, 403)) {
        $anonBlocked = $true
    }
}
if (-not $anonBlocked) {
    throw "SECURITY ERROR: Anonymous request was not blocked on /api/admin/reports!"
}
Write-Host "-> PASS: Anonymous request blocked (401/403)."

# 2.2 Staff access -> Expect 403 Forbidden
$staffSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null = Invoke-WebRequest "$base/auth/login" -WebSession $staffSession -UseBasicParsing
$staffCsrf = $staffSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
$staffBody = '{"username":"staff_demo@motorent.vn","password":"staff123"}'
$staffLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $staffSession -Headers @{'X-XSRF-TOKEN'=$staffCsrf} -ContentType 'application/json' -Body $staffBody
$staffToken = $staffLogin.result.token

$staffBlocked = $false
try {
    $res = Invoke-WebRequest "$base/api/admin/reports" -WebSession $staffSession -Headers @{Authorization="Bearer $staffToken"} -UseBasicParsing
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        $staffBlocked = $true
    }
}
if (-not $staffBlocked) {
    throw "SECURITY ERROR: Staff was not blocked with 403 on /api/admin/reports!"
}
Write-Host "-> PASS: Staff is blocked with 403 Forbidden."

# 3. Login Admin and verify Reports API (Cases 59, 60, 64)
Write-Host "3. Login Admin and verify cash flow financial metrics..."
$adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null = Invoke-WebRequest "$base/auth/login" -WebSession $adminSession -UseBasicParsing
$adminCsrf = $adminSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
$adminBody = '{"username":"admin_demo@motorent.vn","password":"admin123"}'
$adminLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $adminSession -Headers @{'X-XSRF-TOKEN'=$adminCsrf} -ContentType 'application/json' -Body $adminBody
$adminToken = $adminLogin.result.token
$adminHeaders = @{Authorization="Bearer $adminToken"; 'X-XSRF-TOKEN'=$adminCsrf}

$report = Invoke-RestMethod "$base/api/admin/reports" -WebSession $adminSession -Headers $adminHeaders
Write-Host "-> Received report payload successfully:"
$report | ConvertTo-Json -Depth 3 | Write-Host

# 3.1 Check required fields
$requiredFields = @(
    'revenue', 'collected', 'completedOrders', 'totalBikes', 'activeRentals', 'paymentsToReview', 'daily',
    'payosIn', 'cashIn', 'bankTransferIn',
    'rentalCollected', 'rentalRefunded', 'rentalNetRevenue', 'lateFeeCollected',
    'assetDepositIn', 'assetDepositRefunded', 'assetDepositDeducted', 'totalAssetDepositHeld',
    'maintenanceCost', 'operatingProfit'
)
foreach ($f in $requiredFields) {
    if ($null -eq $report.$f) {
        throw "Missing field $f in /api/admin/reports payload!"
    }
}
Write-Host "-> PASS: All 20/20 financial & operational metrics present."

# 3.2 Check Invariants
# Invariant 1: rentalNetRevenue == rentalCollected - rentalRefunded
$expectedNet = [int64]$report.rentalCollected - [int64]$report.rentalRefunded
if ([int64]$report.rentalNetRevenue -ne $expectedNet) {
    throw "Rental net revenue mismatch: rentalNetRevenue=$($report.rentalNetRevenue) != expected=$expectedNet"
}
Write-Host "-> PASS Invariant 1: rentalNetRevenue ($($report.rentalNetRevenue)) = rentalCollected ($($report.rentalCollected)) - rentalRefunded ($($report.rentalRefunded))."

# Invariant 2: totalAssetDepositHeld == assetDepositIn - assetDepositRefunded - assetDepositDeducted
$expectedHeld = [int64]$report.assetDepositIn - [int64]$report.assetDepositRefunded - [int64]$report.assetDepositDeducted
if ([int64]$report.totalAssetDepositHeld -ne $expectedHeld) {
    throw "Asset deposit held mismatch: totalAssetDepositHeld=$($report.totalAssetDepositHeld) != expected=$expectedHeld"
}
Write-Host "-> PASS Invariant 2: totalAssetDepositHeld ($($report.totalAssetDepositHeld)) = in ($($report.assetDepositIn)) - refunded ($($report.assetDepositRefunded)) - deducted ($($report.assetDepositDeducted))."

# Invariant 3: operatingProfit == rentalNetRevenue + lateFeeCollected - maintenanceCost
$expectedProfit = [int64]$report.rentalNetRevenue + [int64]$report.lateFeeCollected - [int64]$report.maintenanceCost
if ([int64]$report.operatingProfit -ne $expectedProfit) {
    throw "Operating profit mismatch: operatingProfit=$($report.operatingProfit) != expected=$expectedProfit"
}
Write-Host "-> PASS Invariant 3: operatingProfit ($($report.operatingProfit)) = netRevenue ($($report.rentalNetRevenue)) + lateFee ($($report.lateFeeCollected)) - maintenance ($($report.maintenanceCost))."

# 4. Check Admin HTML Pages (Cases 61, 62)
Write-Host "4. Checking HTML pages /admin/reports and /admin/dashboard..."
$adminReportPage = Invoke-WebRequest "$base/admin/reports" -WebSession $adminSession -Headers $adminHeaders -UseBasicParsing
if ($adminReportPage.StatusCode -ne 200) {
    throw "Page /admin/reports did not return 200 OK (Status=$($adminReportPage.StatusCode))"
}
if (-not $adminReportPage.Content.Contains('operations-live.js')) {
    throw "Page /admin/reports does not include operations-live.js!"
}
Write-Host "-> PASS: Page /admin/reports loaded 200 OK with operations-live.js."

$adminDashPage = Invoke-WebRequest "$base/admin/dashboard" -WebSession $adminSession -Headers $adminHeaders -UseBasicParsing
if ($adminDashPage.StatusCode -ne 200) {
    throw "Page /admin/dashboard did not return 200 OK (Status=$($adminDashPage.StatusCode))"
}
Write-Host "-> PASS: Page /admin/dashboard loaded 200 OK."

Write-Host "=== ALL PHASE 8 VERIFICATIONS PASSED SUCCESSFULLY! ==="
