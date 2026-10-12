$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8084'

function LoginRole($user, $password) {
    $script:webSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $webSession -UseBasicParsing
    $csrf = $webSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $login = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $webSession -Headers @{'X-XSRF-TOKEN' = $csrf } -ContentType 'application/json' -Body (@{username = $user; password = $password } | ConvertTo-Json)
    $script:auth = @{Authorization = "Bearer $($login.result.token)" }
    $null = Invoke-WebRequest "$base/customer/home" -WebSession $webSession -Headers $auth -UseBasicParsing
    $auth['X-XSRF-TOKEN'] = $webSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $script:currentUserId = $login.result.userId
}

function Api($path, $method = 'GET', $body = $null) {
    $args = @{Uri = "$base$path"; Method = $method; WebSession = $webSession; Headers = $auth }
    if ($null -ne $body) { $args.ContentType = 'application/json'; $args.Body = $body | ConvertTo-Json -Depth 8 }
    Invoke-RestMethod @args
}

function ApiExpectError($path, $method = 'POST', $body = $null) {
    try {
        $args = @{Uri = "$base$path"; Method = $method; WebSession = $webSession; Headers = $auth }
        if ($null -ne $body) { $args.ContentType = 'application/json'; $args.Body = $body | ConvertTo-Json -Depth 8 }
        $res = Invoke-WebRequest @args -UseBasicParsing
        return [int]$res.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw $_
    }
}

function LogoutRole {
    try { $null = Api '/api/auth/logout' 'POST' } catch {}
}

# 1. Staff finds fixture order
LoginRole 'staff_demo@motorent.vn' 'staff123'
$all = Api '/api/operations/data/rental-orders'
$order = @($all | Where-Object { $_.orderCode -eq 'PHASE5-UI-20260929' })
if ($order.Count -ne 1) { throw 'Expected exactly one dedicated clone fixture PHASE5-UI-20260929' }
$id = $order[0].id
$original = Api "/api/rental-orders/$id"
if ($original.status -ne 'HANDED_OVER' -or $original.motorbike.id -ne 9) {
    throw "Fixture order must be HANDED_OVER with bike 9, got status=$($original.status), bike=$($original.motorbike.id)"
}
Write-Output "PASS located fixture order id=$id with status=$($original.status), bike=$($original.motorbike.id)"
LogoutRole

# 2. Customer forbidden from proposing during-rental changes
LoginRole 'customer_ui_verify@example.test' 'customer123'
$changeTime = [DateTime]::Parse($original.returnDate).ToString("yyyy-MM-ddTHH:mm:ss")
$returnTime = [DateTime]::Parse($original.returnDate).AddDays(2).ToString("yyyy-MM-ddTHH:mm:ss")
$reqBody = @{
    motorbikeId = 7
    plannedChangeTime = $changeTime
    newReturnTime = $returnTime
    vehicleChangeFee = 50000
    exchangeAddress = 'Cua hang MotoRent'
    reason = 'Khach muon doi sang xe ga'
}
$status = ApiExpectError "/api/operations/orders/$id/during-rental-changes" 'POST' $reqBody
if ($status -ne 403) { throw "Customer must receive 403 on during-rental proposal, got $status" }
Write-Output 'PASS customer forbidden from proposing during-rental changes (403)'
LogoutRole

# 3. Staff proposes during-rental change
LoginRole 'staff_demo@motorent.vn' 'staff123'
$options = Api "/api/operations/orders/$id/during-rental-replacement-options?changeTime=$changeTime&returnTime=$returnTime"
$hasVision = @($options | Where-Object { $_.motorbikeId -eq 7 })
if ($hasVision.Count -eq 0) { throw 'Bike 7 must be in replacement options' }
Write-Output 'PASS replacement options contain available bike 7'

$quote = Api "/api/operations/orders/$id/during-rental-changes/quote" 'POST' $reqBody
if ($quote.additionalDeposit -ne 1000000) { throw "Expected additional deposit 1000000, got $($quote.additionalDeposit)" }
Write-Output "PASS quote calculated correctly: additionalPayment=$($quote.additionalPayment), additionalDeposit=$($quote.additionalDeposit)"

$prop = Api "/api/operations/orders/$id/during-rental-changes" 'POST' $reqBody
$changeId = $prop.id
if (!$changeId) { throw 'Expected changeId in proposal response' }
Write-Output "PASS Staff created proposal changeId=$changeId"

$orderAfterProp = Api "/api/rental-orders/$id"
if ($orderAfterProp.status -ne 'HANDED_OVER' -or $orderAfterProp.motorbike.id -ne 9) {
    throw "Original order status or vehicle changed before payment!"
}
Write-Output 'PASS original order status (HANDED_OVER) and bike (9) unchanged while WAITING_FOR_PAYMENT'
LogoutRole

# 4. Customer views proposal and attempts payment
LoginRole 'customer_ui_verify@example.test' 'customer123'
$history = Api "/api/rental-orders/$id/vehicle-changes"
$cItem = @($history | Where-Object { $_.id -eq $changeId })[0]
if ($cItem.status -ne 'WAITING_FOR_PAYMENT' -or $cItem.newVehicleId -ne 7) {
    throw "Customer cannot see proposal with status WAITING_FOR_PAYMENT: $($cItem | ConvertTo-Json)"
}
Write-Output 'PASS Customer sees proposal WAITING_FOR_PAYMENT for bike 7'

$status = ApiExpectError "/api/payment/during-rental-change/$changeId" 'POST'
if ($status -ne 503) { throw "Clone payment must return 503 (disabled), got $status" }
Write-Output 'PASS clone clearly reports live payment disabled (503)'
LogoutRole

# 5. Simulate payment success (PayOS callback simulated via fixture helper)
$motoJava = 'C:/Program Files/Java/jdk-21/bin/java.exe'
$motoJdbc = 'C:/Users/THINKPAD789/.m2/repository/com/mysql/mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar'
& $motoJava -cp $motoJdbc docs/phase5-20260929/Phase5Fixture.java simulate-paid $changeId

LoginRole 'staff_demo@motorent.vn' 'staff123'
$orderAfterPaid = Api "/api/rental-orders/$id"
if ($orderAfterPaid.status -ne 'EXTENDED' -or $orderAfterPaid.motorbike.id -ne 9) {
    throw "After payment, status must be EXTENDED and physical bike must STILL be 9 before staff confirmation!"
}
Write-Output 'PASS after payment: status is EXTENDED, physical bike is still 9 (not yet swapped)'

# 6. Staff confirms physical exchange
$confirmBody = @{
    additionalDeposit = 1000000
    method = 'CASH'
    reference = 'PHASE5-ASSET-CONFIRM'
}
$confRes = Api "/api/operations/during-rental-changes/$changeId/confirm" 'POST' $confirmBody
Write-Output "PASS staff confirmed physical exchange: $($confRes.message)"

# Verify vehicle swap in database
$orderAfterConf = Api "/api/rental-orders/$id"
if ($orderAfterConf.motorbike.id -ne 7) {
    throw "Order vehicle must now be bike 7, got $($orderAfterConf.motorbike.id)"
}
Write-Output 'PASS order motorbike successfully swapped to bike 7'

# Verify payment record for additional deposit
$pList = Api "/api/payment/order/$id"
$addDep = @($pList | Where-Object { $_.transactionType -eq 'ASSET_DEPOSIT_ADDITIONAL' })
if ($addDep.Count -ne 1 -or $addDep[0].amount -ne 1000000 -or $addDep[0].direction -ne 'IN') {
    throw "ASSET_DEPOSIT_ADDITIONAL payment record missing or invalid: $($addDep | ConvertTo-Json)"
}
Write-Output 'PASS ASSET_DEPOSIT_ADDITIONAL payment recorded (1,000,000 VND, IN, PAID)'

# 7. Idempotency: duplicate confirm call
$confRes2 = Api "/api/operations/during-rental-changes/$changeId/confirm" 'POST' $confirmBody
$pListAfter = Api "/api/payment/order/$id"
if ($pListAfter.Count -ne $pList.Count) {
    throw "Duplicate confirm created duplicate payment!"
}
Write-Output 'PASS duplicate confirm call is idempotent; payment count unchanged'

LogoutRole

# 8. Customer views updated rental detail page
LoginRole 'customer_ui_verify@example.test' 'customer123'
$detailPage = Invoke-WebRequest "$base/customer/rental-detail/$id" -WebSession $webSession -Headers $auth -UseBasicParsing
if ($detailPage.StatusCode -ne 200) { throw "Detail page returned status $($detailPage.StatusCode)" }
Write-Output 'PASS Customer rental detail rendered 200 OK'
LogoutRole

Write-Output 'ALL PHASE 5 DURING-RENTAL HTTP CHECKS PASSED'
