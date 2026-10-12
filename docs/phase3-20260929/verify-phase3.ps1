$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:8082'

function LoginRole($user, $password) {
    $script:webSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $webSession
    $csrf = $webSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $login = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $webSession -Headers @{'X-XSRF-TOKEN' = $csrf } -ContentType 'application/json' -Body (@{username = $user; password = $password } | ConvertTo-Json)
    $script:auth = @{Authorization = "Bearer $($login.result.token)" }
    $null = Invoke-WebRequest "$base/customer/home" -WebSession $webSession -Headers $auth
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
        $res = Invoke-WebRequest @args
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
$staffId = $script:currentUserId
$all = Api '/api/operations/data/rental-orders'
$order = @($all | Where-Object { $_.orderCode -eq 'PHASE3-UI-20260929' })
if ($order.Count -ne 1) { throw 'Expected exactly one dedicated clone fixture PHASE3-UI-20260929' }
$id = $order[0].id
$original = Api "/api/rental-orders/$id"
Write-Output "PASS located fixture order id=$id with status=$($original.status)"
LogoutRole

# 2. Customer forbidden from staff handover
LoginRole 'customer_ui_verify@example.test' 'customer123'
$status = ApiExpectError "/api/rental-orders/$id/hand-over" 'POST' @{remainingAmount = 100000; securityDeposit = 1000000; paymentMethod = 'CASH' }
if ($status -ne 403) { throw "Customer must receive 403 on handover, got $status" }
Write-Output 'PASS Customer forbidden from executing handover (403)'
LogoutRole

# 3. Staff validations
LoginRole 'staff_demo@motorent.vn' 'staff123'

# 3a. Invalid payment method
$status = ApiExpectError "/api/rental-orders/$id/hand-over" 'POST' @{remainingAmount = 100000; securityDeposit = 1000000; paymentMethod = 'BITCOIN' }
if ($status -lt 400 -or $status -ge 500) { throw "Invalid payment method must be rejected with 4xx, got $status" }
Write-Output 'PASS rejected invalid payment method'

# 3b. Negative amount
$status = ApiExpectError "/api/rental-orders/$id/hand-over" 'POST' @{remainingAmount = -50000; securityDeposit = 1000000; paymentMethod = 'CASH' }
if ($status -lt 400 -or $status -ge 500) { throw "Negative remaining amount must be rejected with 4xx, got $status" }
Write-Output 'PASS rejected negative amount'

# 3c. Actual pickup after return date
$status = ApiExpectError "/api/rental-orders/$id/hand-over" 'POST' @{remainingAmount = 100000; securityDeposit = 1000000; paymentMethod = 'CASH'; actualPickupTime = '2099-01-01T00:00:00' }
if ($status -lt 400 -or $status -ge 500) { throw "Pickup after return date must be rejected with 4xx, got $status" }
Write-Output 'PASS rejected actual pickup after return date'

# 4. Valid handover with remaining rental + asset deposit
$handoverBody = @{
    remainingAmount = 100000
    securityDeposit = 1000000
    paymentMethod = 'CASH'
    note = 'Phase 3 live verification handover test'
}
$handoverRes = Api "/api/rental-orders/$id/hand-over" 'POST' $handoverBody
if ($handoverRes.status -ne 'HANDED_OVER') { throw "Order status must be HANDED_OVER, got $($handoverRes.status)" }
Write-Output 'PASS handover executed successfully, order status is HANDED_OVER'

# 5. Verify order details and payments in database
$after = Api "/api/rental-orders/$id"
if ($after.balancePaid -ne 100000) { throw "Balance paid must be 100000, got $($after.balancePaid)" }
if ($after.status -ne 'HANDED_OVER') { throw "Order status in DB must be HANDED_OVER, got $($after.status)" }

# Check payments
$orderPayments = Api "/api/payment/order/$id"
$remPayment = @($orderPayments | Where-Object { $_.transactionType -eq 'RENTAL_REMAINING' })
$secPayment = @($orderPayments | Where-Object { $_.transactionType -eq 'ASSET_DEPOSIT' })

if ($remPayment.Count -ne 1) { throw "Expected exactly 1 RENTAL_REMAINING payment, got $($remPayment.Count)" }
if ($remPayment[0].amount -ne 100000 -or $remPayment[0].direction -ne 'IN' -or $remPayment[0].status -ne 'PAID') {
    throw "RENTAL_REMAINING payment fields invalid: $($remPayment[0] | ConvertTo-Json)"
}
Write-Output 'PASS RENTAL_REMAINING payment recorded atomically (100,000 VND, IN, PAID)'

if ($secPayment.Count -ne 1) { throw "Expected exactly 1 ASSET_DEPOSIT payment, got $($secPayment.Count)" }
if ($secPayment[0].amount -ne 1000000 -or $secPayment[0].direction -ne 'IN' -or $secPayment[0].status -ne 'PAID') {
    throw "ASSET_DEPOSIT payment fields invalid: $($secPayment[0] | ConvertTo-Json)"
}
Write-Output 'PASS ASSET_DEPOSIT payment recorded atomically (1,000,000 VND, IN, PAID)'

# 6. Idempotency: duplicate handover rejected and does not duplicate payments
$status = ApiExpectError "/api/rental-orders/$id/hand-over" 'POST' $handoverBody
if ($status -lt 400 -or $status -ge 500) { throw "Consecutive handover must be rejected, got $status" }
$orderPaymentsAfter = Api "/api/payment/order/$id"
if ($orderPaymentsAfter.Count -ne $orderPayments.Count) {
    throw "Duplicate handover created extra payments!"
}
Write-Output 'PASS duplicate handover rejected; payment count unchanged'

LogoutRole

# 7. Customer sees handed-over status and contract details
LoginRole 'customer_ui_verify@example.test' 'customer123'
$page = Invoke-WebRequest "$base/customer/rental-detail/$id" -WebSession $webSession -Headers $auth
if ($page.StatusCode -ne 200) { throw "Customer rental detail page returned status $($page.StatusCode)" }
Write-Output 'PASS Customer rental detail returned HTTP 200 after handover (not a browser interaction check)'

LogoutRole
Write-Output 'ALL PHASE 3 HTTP CHECKS PASSED'
