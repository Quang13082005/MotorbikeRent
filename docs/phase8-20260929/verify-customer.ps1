$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8087'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null=Invoke-WebRequest "$base/auth/login" -WebSession $session -UseBasicParsing
function Csrf { $session.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value }
$login=Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $session -Headers @{'X-XSRF-TOKEN'=(Csrf)} -ContentType 'application/json' -Body '{"username":"customer_ui_verify@example.test","password":"customer123"}'
$headers=@{Authorization="Bearer $($login.result.token)"}
$null=Invoke-WebRequest "$base/customer/rental-contract/7" -WebSession $session -Headers $headers -UseBasicParsing
$headers['X-XSRF-TOKEN']=Csrf
foreach($test in @(@{id=9;amount=1000000},@{id=7;amount=2000000},@{id=12;amount=1500000})){
 foreach($option in @(1,2)){
  $payload=@{motorbikeId=$test.id;pickupDate=(Get-Date).AddDays(2).ToString('yyyy-MM-ddTHH:mm');returnDate=(Get-Date).AddDays(3).ToString('yyyy-MM-ddTHH:mm');securityOption=$option;pickupType='STORE';returnType='STORE'}
  $quote=Invoke-RestMethod "$base/api/rental-orders/quote" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body ($payload|ConvertTo-Json)
  $expected=if($option-eq 2){0}else{$test.amount}
  if($quote.securityDeposit-ne $expected){throw 'Wrong security deposit'}
  Write-Output "PASS MySQL bike=$($test.id) option=$option security=$expected"
 }
}
$payload.pickupDate=(Get-Date).AddDays(-1).ToString('yyyy-MM-ddTHH:mm')
$status=try{(Invoke-WebRequest "$base/api/rental-orders/quote" -Method Post -WebSession $session -Headers $headers -ContentType 'application/json' -Body ($payload|ConvertTo-Json) -UseBasicParsing).StatusCode}catch{[int]$_.Exception.Response.StatusCode}
if($status -ne 400){throw "Past quote status $status"}
Write-Output 'PASS past quote rejected by server'
foreach($path in @('/customer/profile','/customer/rental-history','/customer/my-rentals','/customer/rental-contract/7')){
 $page=Invoke-WebRequest ($base+$path) -WebSession $session -Headers $headers -UseBasicParsing
 if($page.StatusCode-ne 200){throw 'Page failed'}
 Write-Output "PASS render $path"
}
$status=try{(Invoke-WebRequest "$base/api/payment/create/0" -Method Post -WebSession $session -Headers $headers -UseBasicParsing).StatusCode}catch{[int]$_.Exception.Response.StatusCode}
if($status -ne 503){throw 'Clone payment must be disabled'}
Write-Output 'PASS clone clearly rejects live payment'
$headers['X-XSRF-TOKEN']=Csrf
$null=Invoke-RestMethod "$base/api/auth/logout" -Method Post -WebSession $session -Headers $headers
Write-Output 'ALL CUSTOMER HTTP CHECKS PASSED ON PORT 8087'
