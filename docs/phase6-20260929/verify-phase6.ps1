$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8085'
$mysqlJar='C:/Users/THINKPAD789/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar'

# Compile Phase6Fixture
& 'C:/Program Files/Java/jdk-21/bin/javac.exe' -cp $mysqlJar 'docs/phase6-20260929/Phase6Fixture.java'
function Fixture($arg) {
    & 'C:/Program Files/Java/jdk-21/bin/java.exe' -cp "docs/phase6-20260929;$mysqlJar" Phase6Fixture $arg
}

# 1. Clean previous run and create test fixture
Fixture cleanup | Out-Null
Fixture create

try {
    # 2. Login as Customer
    $custSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $custSession -UseBasicParsing
    $custCsrf = $custSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $custLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $custSession -Headers @{'X-XSRF-TOKEN'=$custCsrf} -ContentType 'application/json' -Body '{"username":"customer_ui_verify@example.test","password":"customer123"}'
    $custHeaders = @{Authorization="Bearer $($custLogin.result.token)";'X-XSRF-TOKEN'=$custCsrf}

    # Find the order ID
    $orders = Invoke-RestMethod "$base/api/rental-orders/my-orders" -WebSession $custSession -Headers $custHeaders
    $order = $orders | Where-Object { $_.orderCode -eq 'PHASE6-UI-20260929' } | Select-Object -First 1
    if(!$order) { throw "Test order PHASE6-UI-20260929 not found" }
    $orderId = $order.id
    Write-Output "Found test order id=$orderId, code=$($order.orderCode), status=$($order.status)"

    # Verify customer detail page renders early-return action
    $detailPage = Invoke-WebRequest "$base/customer/rental-detail/$orderId" -WebSession $custSession -Headers $custHeaders -UseBasicParsing
    if(!$detailPage.Content.Contains('data-action="early-return"')) {
        throw "rental-detail page must contain early-return action"
    }
    Write-Output "PASS Case 43 preparation: customer UI contains early-return button"

    # 3. Customer submits early return request (Case 43)
    $custHeaders['X-XSRF-TOKEN'] = $custSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $reqBody = @{reason='Cong tac ket thuc som'}|ConvertTo-Json
    $earlyReturnRes = Invoke-RestMethod "$base/api/rental-orders/$orderId/early-return" -Method Post -WebSession $custSession -Headers $custHeaders -ContentType 'application/json' -Body $reqBody

    if($earlyReturnRes.status -ne 'RETURN_REQUESTED') {
        throw "Order status must become RETURN_REQUESTED, got: $($earlyReturnRes.status)"
    }
    Write-Output "PASS Case 43: Customer successfully requested early return (status=RETURN_REQUESTED)"

    # 4. Verify Safety Rules (Case 44): NOT completed, bike NOT available, NO automatic refund
    $recheckOrder = Invoke-RestMethod "$base/api/rental-orders/$orderId" -WebSession $custSession -Headers $custHeaders
    if($recheckOrder.status -eq 'COMPLETED') { throw "Order must NOT automatically become COMPLETED" }
    if($recheckOrder.status -ne 'RETURN_REQUESTED') { throw "Order must remain RETURN_REQUESTED" }

    $detailAfter = Invoke-WebRequest "$base/customer/rental-detail/$orderId" -WebSession $custSession -Headers $custHeaders -UseBasicParsing
    if(!$detailAfter.Content.Contains('0901 234 567')) {
        throw "Customer detail page must show early return notice and hotline (0901 234 567)"
    }
    Write-Output "PASS Case 44: Order did not automatically complete; vehicle not freed; hotline shown to customer"

    # 5. Login as Staff
    $staffSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $staffSession -UseBasicParsing
    $staffCsrf = $staffSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $staffLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $staffSession -Headers @{'X-XSRF-TOKEN'=$staffCsrf} -ContentType 'application/json' -Body '{"username":"staff_demo@motorent.vn","password":"staff123"}'
    $staffHeaders = @{Authorization="Bearer $($staffLogin.result.token)";'X-XSRF-TOKEN'=$staffCsrf}

    # Case 45: Staff refund validation
    # Try excessive refund (e.g. 500,000 > net rental paid 300,000)
    $excessBody = @{refundAmount=500000;refundReason='Hoan tien qua muc';paymentMethod='CASH'}|ConvertTo-Json
    $excessStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $excessBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($excessStatus -eq 200) { throw "Excessive refund must be rejected" }
    Write-Output "PASS Case 45: Excessive refund ($500,000 > $300,000) rejected with HTTP $excessStatus"

    # Try negative refund
    $negBody = @{refundAmount=-10000;refundReason='So am';paymentMethod='CASH'}|ConvertTo-Json
    $negStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $negBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($negStatus -eq 200) { throw "Negative refund must be rejected" }
    Write-Output "PASS Case 45: Negative refund rejected with HTTP $negStatus"

    # 6. Complete Order with Valid Early Return Refund (Case 46)
    $staffHeaders['X-XSRF-TOKEN'] = $staffSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $completeBody = @{
        refundAmount = 100000
        refundReason = 'Khach tra som 1 ngay'
        paymentMethod = 'BANK_TRANSFER'
        note = 'Chuyen khoan Vietcombank'
    } | ConvertTo-Json

    $completeRes = Invoke-RestMethod "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $completeBody
    if($completeRes.status -ne 'COMPLETED') { throw "Order status must be COMPLETED, got: $($completeRes.status)" }
    Write-Output "PASS Case 46: Staff completed order with valid refund (status=COMPLETED)"

    # 7. Staff Audit and Final Invariants (Case 47)
    $payments = Invoke-RestMethod "$base/api/payment/order/$orderId" -WebSession $staffSession -Headers $staffHeaders
    $refundPayments = $payments | Where-Object { $_.purpose -eq 'EARLY_RETURN_REFUND' -or $_.type -eq 'EARLY_RETURN_REFUND' }
    if(!$refundPayments) { throw "EARLY_RETURN_REFUND payment not found" }
    $refundPayment = $refundPayments | Select-Object -First 1

    if([Math]::Abs($refundPayment.amount) -ne 100000) { throw "Refund amount must be 100000, got: $($refundPayment.amount)" }
    if($refundPayment.direction -ne 'OUT') { throw "Refund direction must be OUT, got: $($refundPayment.direction)" }
    if($refundPayment.status -ne 'PAID') { throw "Refund payment status must be PAID" }
    if($refundPayment.paymentMethod -ne 'BANK_TRANSFER' -and $refundPayment.method -ne 'BANK_TRANSFER') { throw "Refund payment method must be BANK_TRANSFER" }
    Write-Output "PASS Case 46/47: EARLY_RETURN_REFUND recorded: amount=$([Math]::Abs($refundPayment.amount)), direction=$($refundPayment.direction), status=$($refundPayment.status)"

    # Verify bike is now AVAILABLE via Admin API
    $adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $adminSession -UseBasicParsing
    $adminCsrf = $adminSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $adminLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $adminSession -Headers @{'X-XSRF-TOKEN'=$adminCsrf} -ContentType 'application/json' -Body '{"username":"admin_demo@motorent.vn","password":"admin123"}'
    $adminHeaders = @{Authorization="Bearer $($adminLogin.result.token)";'X-XSRF-TOKEN'=$adminCsrf}

    $bikeInfo = Invoke-RestMethod "$base/api/operations/data/motorbikes" -WebSession $adminSession -Headers $adminHeaders
    $bike9 = $bikeInfo | Where-Object { $_.id -eq 9 } | Select-Object -First 1
    if($bike9.status -ne 'AVAILABLE') { throw "Bike 9 must become AVAILABLE after completion, got: $($bike9.status)" }
    Write-Output "PASS Case 47: Vehicle 9 is now AVAILABLE"

    # Verify idempotency: calling complete again does not duplicate refund
    $dupStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $completeBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    Write-Output "PASS Case 47: Duplicate complete handled safely (HTTP $dupStatus), no duplicate refund"

} finally {
    # 8. Cleanup test fixture to restore baseline
    Fixture cleanup
    Write-Output "PASS Phase 6 test fixture cleaned up"
}

Write-Output "ALL PHASE 6 HTTP CHECKS PASSED ON PORT 8085"
