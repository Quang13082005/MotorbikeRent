$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8082'
function LoginRole($user,$password){
 $script:webSession=New-Object Microsoft.PowerShell.Commands.WebRequestSession
 $null=Invoke-WebRequest "$base/auth/login" -WebSession $webSession
 $csrf=$webSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
 $login=Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $webSession -Headers @{'X-XSRF-TOKEN'=$csrf} -ContentType 'application/json' -Body (@{username=$user;password=$password}|ConvertTo-Json)
 $script:auth=@{Authorization="Bearer $($login.result.token)"}
 $null=Invoke-WebRequest "$base/customer/home" -WebSession $webSession -Headers $auth
 $auth['X-XSRF-TOKEN']=$webSession.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
}
function Api($path,$method='GET',$body=$null){
 $args=@{Uri="$base$path";Method=$method;WebSession=$webSession;Headers=$auth}
 if($null-ne $body){$args.ContentType='application/json';$args.Body=$body|ConvertTo-Json -Depth 8}
 Invoke-RestMethod @args
}
function LogoutRole { $null=Api '/api/auth/logout' 'POST' }
LoginRole 'staff_demo@motorent.vn' 'staff123'
$all=Api '/api/operations/data/rental-orders'
$order=@($all|Where-Object {$_.orderCode-eq 'PHASE2-UI-20260929'})
if($order.Count-ne 1){throw 'Expected exactly one dedicated clone fixture'}
$id=$order[0].id
$original=Api "/api/rental-orders/$id"
$options=Api "/api/operations/orders/$id/replacement-options"
$dearer=@($options|Where-Object {$_.additionalPayment-gt 0})[0]
$body=@{motorbikeId=$dearer.newVehicleId;reason='Phase2 isolated API verification'}
$q=Api "/api/operations/orders/$id/vehicle-changes/quote" 'POST' $body
$c=Api "/api/operations/orders/$id/vehicle-changes" 'POST' $body
$after=Api "/api/rental-orders/$id"
if($c.status-ne 'WAITING_FOR_PAYMENT'-or $after.motorbike.id-ne $original.motorbike.id-or $after.totalPrice-ne $original.totalPrice-or $q.additionalPayment-ne $c.additionalPayment){throw 'Unpaid replacement changed original order'}
Write-Output 'PASS live clone proposal quote and unchanged original order/payment before top-up'
LogoutRole
LoginRole 'customer_ui_verify@example.test' 'customer123'
$history=Api "/api/rental-orders/$id/vehicle-changes"
if(!@($history|Where-Object {$_.id-eq $c.id-and $_.status-eq 'WAITING_FOR_PAYMENT'})){throw 'Customer cannot see proposal'}
foreach($route in @("/customer/rental-detail/$id","/customer/payment-result?orderId=$id")){
 $page=Invoke-WebRequest "$base$route" -WebSession $webSession -Headers $auth
 if($page.StatusCode-ne 200){throw 'Customer detail render failed'}
}
$r=Invoke-WebRequest "$base/api/payment/vehicle-change/$($c.id)" -Method Post -WebSession $webSession -Headers $auth -SkipHttpErrorCheck
if($r.StatusCode-ne 503){throw 'Clone must reject real PayOS top-up'}
$r=Invoke-WebRequest "$base/api/operations/orders/$id/replacement-options" -WebSession $webSession -Headers $auth -SkipHttpErrorCheck
if($r.StatusCode-ne 403){throw 'Customer must not act as Staff'}
Write-Output 'PASS customer proposal/detail/payment-result rendering, clone payment disabled, operator permission denied'
LogoutRole
LoginRole 'admin_demo@motorent.vn' 'admin123'
$null=Api "/api/operations/orders/$id/replacement-options"
Write-Output 'PASS Admin replacement API allowed'
LogoutRole
LoginRole 'staff_demo@motorent.vn' 'staff123'
$null=Api "/api/operations/vehicle-changes/$($c.id)" 'DELETE'
$history=Api "/api/rental-orders/$id/vehicle-changes"
$cancelled=@($history|Where-Object {$_.id-eq $c.id})[0]
if($cancelled.status-ne 'CANCELLED'-or !$cancelled.cancelledByStaffId){throw 'Cancellation audit missing'}
$options=Api "/api/operations/orders/$id/replacement-options"
$same=@($options|Where-Object {$_.newTotal-eq $original.totalPrice})[0]
if(!$same){throw 'Same price replacement needed for fixture'}
$applied=Api "/api/operations/orders/$id/vehicle-changes" 'POST' @{motorbikeId=$same.newVehicleId;reason='Phase2 equal-price verification'}
$after=Api "/api/rental-orders/$id"
if($applied.status-ne 'APPLIED'-or $after.id-ne $id-or $after.depositPaid-ne $original.depositPaid-or $after.payments.Count-ne $original.payments.Count){throw 'Same price change did not preserve original booking'}
Write-Output 'PASS cancellation audit and same-price change preserves rental ID and original payment'
$null=Api "/api/operations/orders/$id/reject-with-refund" 'POST' @{amount=$after.depositPaid;method='BANK_TRANSFER';reference='PHASE2-UI-REFUND-20260929';reason='Disposable clone test: refund already simulated';note='No real bank transaction'}
$after=Api "/api/rental-orders/$id"
if($after.status-ne 'REJECTED'-or $after.depositPaid-ne 0){throw 'Refund/rejection failed'}
Write-Output 'PASS actual-confirmation refund creates final rejection with zero rental paid balance'
LogoutRole
