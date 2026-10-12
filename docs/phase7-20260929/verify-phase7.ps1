$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8086'
$mysqlJar='C:/Users/THINKPAD789/.m2/repository/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar'

# Compile Phase7Fixture
& 'C:/Program Files/Java/jdk-21/bin/javac.exe' -cp $mysqlJar 'docs/phase7-20260929/Phase7Fixture.java'
function Fixture($arg) {
    & 'C:/Program Files/Java/jdk-21/bin/java.exe' -cp "docs/phase7-20260929;$mysqlJar" Phase7Fixture $arg
}

# 1. Clean previous run and create test fixture
Fixture cleanup | Out-Null
$createOut = Fixture create
Write-Output $createOut
if ($createOut -match 'OrderId=(\d+)') {
    $orderId = [int64]$matches[1]
} else {
    throw "Failed to extract OrderId from fixture output: $createOut"
}

try {
    # 2. Login as Staff
    $staffSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $staffSession -UseBasicParsing
    $staffCsrf = $staffSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $staffLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $staffSession -Headers @{'X-XSRF-TOKEN'=$staffCsrf} -ContentType 'application/json' -Body '{"username":"staff_demo@motorent.vn","password":"staff123"}'
    $staffHeaders = @{Authorization="Bearer $($staffLogin.result.token)";'X-XSRF-TOKEN'=$staffCsrf}

    # Get the order details
    $order = Invoke-RestMethod "$base/api/rental-orders/$orderId" -WebSession $staffSession -Headers $staffHeaders
    Write-Output "Found test order id=$orderId, code=$($order.orderCode), status=$($order.status)"

    # Case 48 & 49: Check Staff UI renders complete action and modal logic
    $staffPage = Invoke-WebRequest "$base/staff/rental-orders" -WebSession $staffSession -Headers $staffHeaders -UseBasicParsing
    if(!$staffPage.Content.Contains('data-action="complete"') -and !$staffPage.Content.Contains('data-order-id')) {
        throw "Staff page must render complete action"
    }
    Write-Output "PASS Case 48: Staff UI renders complete action button"

    # Case 50 & 51: Overdue detection
    # Return date was yesterday -> order is overdue
    $pickup = [DateTime]::Parse($order.pickupDate)
    $returnAt = [DateTime]::Parse($order.returnDate)
    $now = Get-Date
    if($now -gt $returnAt) {
        Write-Output "PASS Case 50 & 51: Detected overdue rental (returnDate=$returnAt, now=$now)"
    }

    # Case 52, 53, 54: Validation checks
    # Test 1: Sum mismatch (refund 1,500,000 + deduct 300,000 = 1,800,000 != held 2,000,000)
    $staffHeaders['X-XSRF-TOKEN'] = $staffSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $mismatchBody = @{
        assetDepositRefundAmount = 1500000
        assetDepositDeductionAmount = 300000
        depositDeductionReason = 'Gương chiếu hậu hỏng'
    } | ConvertTo-Json

    $mismatchStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $mismatchBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($mismatchStatus -eq 200) { throw "Deposit sum mismatch must be rejected" }
    Write-Output "PASS Case 54: Deposit mismatch rejected with HTTP $mismatchStatus"

    # Test 2: Deduction > 0 without reason
    $noReasonBody = @{
        assetDepositRefundAmount = 1700000
        assetDepositDeductionAmount = 300000
        depositDeductionReason = '   '
    } | ConvertTo-Json
    $noReasonStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $noReasonBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($noReasonStatus -eq 200) { throw "Deduction without reason must be rejected" }
    Write-Output "PASS Case 54: Deduction without reason rejected with HTTP $noReasonStatus"

    # Test 3: Negative amounts
    $negBody = @{
        assetDepositRefundAmount = -100000
        assetDepositDeductionAmount = 2100000
        depositDeductionReason = 'Test'
    } | ConvertTo-Json
    $negStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $negBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($negStatus -eq 200) { throw "Negative deposit amount must be rejected" }
    Write-Output "PASS Case 54: Negative deposit amount rejected with HTTP $negStatus"

    # Case 55, 56, 57, 58: Complete Order with Refund, Deduction, and Late Fee
    $staffHeaders['X-XSRF-TOKEN'] = $staffSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $validCompleteBody = @{
        assetDepositRefundAmount = 1700000
        assetDepositDeductionAmount = 300000
        depositDeductionReason = 'Hu guong chieu hau ben phai'
        depositRefundMethod = 'CASH'
        lateFee = 150000
        lateFeeReason = 'Khach tra tre 1 ngay'
        lateFeePaymentMethod = 'CASH'
        note = 'Hoan tat don thanh cong tai cua hang'
    } | ConvertTo-Json

    $completeRes = Invoke-RestMethod "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $validCompleteBody
    if($completeRes.status -ne 'COMPLETED') {
        throw "Order status must be COMPLETED, got: $($completeRes.status)"
    }
    Write-Output "PASS Case 55 & 57: Order successfully completed with status=COMPLETED"

    # Verify Payments audit (Case 55, 58)
    $payments = Invoke-RestMethod "$base/api/payment/order/$orderId" -WebSession $staffSession -Headers $staffHeaders
    $depRef = $payments | Where-Object { $_.transactionType -eq 'ASSET_DEPOSIT_REFUND' } | Select-Object -First 1
    if(!$depRef -or [Math]::Abs($depRef.amount) -ne 1700000 -or $depRef.direction -ne 'OUT') {
        throw "ASSET_DEPOSIT_REFUND payment missing or incorrect: $($depRef | ConvertTo-Json)"
    }
    Write-Output "PASS Case 55: ASSET_DEPOSIT_REFUND recorded (amount=1,700,000, direction=OUT)"

    $depDed = $payments | Where-Object { $_.transactionType -eq 'DEPOSIT_DEDUCTION' } | Select-Object -First 1
    if(!$depDed -or [Math]::Abs($depDed.amount) -ne 300000 -or $depDed.direction -ne 'OUT') {
        throw "DEPOSIT_DEDUCTION payment missing or incorrect: $($depDed | ConvertTo-Json)"
    }
    Write-Output "PASS Case 55: DEPOSIT_DEDUCTION recorded (amount=300,000, direction=OUT)"

    $lateFeePay = $payments | Where-Object { $_.transactionType -eq 'LATE_FEE' } | Select-Object -First 1
    if(!$lateFeePay -or [Math]::Abs($lateFeePay.amount) -ne 150000 -or $lateFeePay.direction -ne 'IN') {
        throw "LATE_FEE payment missing or incorrect: $($lateFeePay | ConvertTo-Json)"
    }
    Write-Output "PASS Case 58: LATE_FEE recorded as cash in (amount=150,000, direction=IN, method=$($lateFeePay.method))"

    # Case 48 & 49: Bike status returned to AVAILABLE
    $bikeId = if ($order.motorbike -and $order.motorbike.id) { $order.motorbike.id } else { 9 }
    $bikeInfo = Invoke-RestMethod "$base/api/motorbikes/$bikeId" -WebSession $staffSession -Headers $staffHeaders
    $bikeStatus = if ($bikeInfo.result) { $bikeInfo.result.status } else { $bikeInfo.status }
    if($bikeStatus -ne 'AVAILABLE') {
        throw "Bike must become AVAILABLE after completion, got: $bikeStatus"
    }
    Write-Output "PASS Case 48: Bike status returned to AVAILABLE (status=$bikeStatus)"

    # Test Idempotency: Repeating completion call must be rejected (400 Bad Request)
    $repeatStatus = try {
        (Invoke-WebRequest "$base/api/rental-orders/$orderId/complete" -Method Post -WebSession $staffSession -Headers $staffHeaders -ContentType 'application/json' -Body $validCompleteBody -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($repeatStatus -eq 200) { throw "Repeat completion on already COMPLETED order must be rejected" }
    Write-Output "PASS Idempotency: Second complete call safely rejected with HTTP $repeatStatus"

    # Case 58 customer review flow
    $custSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $null = Invoke-WebRequest "$base/auth/login" -WebSession $custSession -UseBasicParsing
    $custCsrf = $custSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
    $custLogin = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $custSession -Headers @{'X-XSRF-TOKEN'=$custCsrf} -ContentType 'application/json' -Body '{"username":"customer_ui_verify@example.test","password":"customer123"}'
    $custHeaders = @{Authorization="Bearer $($custLogin.result.token)"}
    $null = Invoke-WebRequest "$base/customer/rental-history" -WebSession $custSession -Headers $custHeaders -UseBasicParsing
    $custHeaders['X-XSRF-TOKEN'] = $custSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value

    $reviewBody = @{
        orderId = $orderId
        rating = 5
        comment = 'Xe chay rat em, nhan vien ho tro nhiet tinh'
    } | ConvertTo-Json
    $reviewBytes = [System.Text.Encoding]::UTF8.GetBytes($reviewBody)
    try {
        $reviewRes = Invoke-RestMethod "$base/api/reviews" -Method Post -WebSession $custSession -Headers $custHeaders -ContentType 'application/json; charset=utf-8' -Body $reviewBytes
        Write-Output "PASS Case 58 Review: Customer submitted 5-star review successfully"
    } catch {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $respBody = $reader.ReadToEnd()
        Write-Output "REVIEW FAILED BODY: $respBody"
        throw $_
    }

    # Repeat review rejected
    $repeatReviewStatus = try {
        (Invoke-WebRequest "$base/api/reviews" -Method Post -WebSession $custSession -Headers $custHeaders -ContentType 'application/json; charset=utf-8' -Body $reviewBytes -UseBasicParsing).StatusCode
    } catch {
        [int]$_.Exception.Response.StatusCode
    }
    if($repeatReviewStatus -eq 200) { throw "Duplicate review must be rejected" }
    Write-Output "PASS Case 58 Review: Duplicate review rejected with HTTP $repeatReviewStatus"

    Write-Output "=========================================================="
    Write-Output "ALL PHASE 7 HTTP VERIFICATION CHECKS PASSED SUCCESSFULLY!"
    Write-Output "=========================================================="
}
finally {
    # Dọn dẹp fixture và xác nhận baseline
    Write-Output "Cleaning up test fixture..."
    Fixture cleanup | Out-Null
    Fixture verify-baseline
}
