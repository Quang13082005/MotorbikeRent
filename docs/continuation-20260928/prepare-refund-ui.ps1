$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8082'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null=Invoke-WebRequest "$base/auth/login" -WebSession $session
$csrf=$session.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
$login=Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $session -Headers @{'X-XSRF-TOKEN'=$csrf} -ContentType 'application/json' -Body '{"username":"staff_demo@motorent.vn","password":"staff123"}'
$null=Invoke-WebRequest "$base/staff/rental-orders" -WebSession $session -Headers @{Authorization="Bearer $($login.result.token)"}
$headers=@{Authorization="Bearer $($login.result.token)";'X-XSRF-TOKEN'=$session.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value}
$orders=Invoke-RestMethod "$base/api/operations/data/rental-orders" -Headers $headers -WebSession $session
$fixture=$orders|Where-Object orderCode -eq 'UI-HANDOVER-20260928'
if(!$fixture){throw 'Missing fixture'}
$null=Invoke-RestMethod "$base/api/rental-orders/$($fixture.id)/hand-over" -Method Post -Headers $headers -WebSession $session -ContentType 'application/json' -Body '{"identityChecked":true,"paymentReceived":true}'
$null=Invoke-RestMethod "$base/api/rental-orders/$($fixture.id)/complete" -Method Post -Headers $headers -WebSession $session
Write-Output "PASS clone fixture completed; security deposit ready for UI refund"
$null=Invoke-RestMethod "$base/api/auth/logout" -Method Post -Headers $headers -WebSession $session
