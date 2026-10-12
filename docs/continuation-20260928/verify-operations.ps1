$ErrorActionPreference='Stop'
$base='http://127.0.0.1:8082'
foreach($role in @('admin','staff')){
 $session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
 $null=Invoke-WebRequest "$base/auth/login" -WebSession $session -UseBasicParsing
 $csrf=$session.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
 $body=@{username="${role}_demo@motorent.vn";password="${role}123"}|ConvertTo-Json
 $login=Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $session -Headers @{'X-XSRF-TOKEN'=$csrf} -ContentType 'application/json' -Body $body
 $headers=@{Authorization="Bearer $($login.result.token)"}
 $pages=if($role-eq 'admin'){@('dashboard','motorbikes','customers','staffs','rental-orders','maintenance','payments','extensions','reviews','reports','settings','travel-places')}else{@('dashboard','rental-orders','maintenance','profile')}
 foreach($page in $pages){
  $r=Invoke-WebRequest "$base/$role/$page" -WebSession $session -Headers $headers -UseBasicParsing
  if($r.StatusCode-ne 200 -or !$r.Content.Contains('id="mainContent"')){throw "Render failed: $role/$page"}
  Write-Output "PASS page $role/$page"
 }
 $modules=if($role-eq 'admin'){@('motorbikes','customers','staffs','rental-orders','maintenance','payments','extensions','reviews','travel-places')}else{@('rental-orders','maintenance','payments','extensions','reviews')}
 foreach($module in $modules){$null=Invoke-RestMethod "$base/api/operations/data/$module" -WebSession $session -Headers $headers;Write-Output "PASS database API $role/$module"}
 if($role-eq 'staff'){
  foreach($route in @('/admin/settings','/api/admin/reports','/api/operations/data/customers','/api/operations/data/staffs','/api/operations/data/travel-places')){
   $code=try{(Invoke-WebRequest ($base+$route) -WebSession $session -Headers $headers -UseBasicParsing).StatusCode}catch{[int]$_.Exception.Response.StatusCode}
   if($code-ne 403){throw "Staff access must be denied: $route (got $code)"}
   Write-Output "PASS staff denied $route"
  }
 }else{$null=Invoke-RestMethod "$base/api/admin/reports" -WebSession $session -Headers $headers;Write-Output 'PASS admin report aggregation'}
 $headers['X-XSRF-TOKEN']=$session.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value
 $null=Invoke-RestMethod "$base/api/auth/logout" -Method Post -WebSession $session -Headers $headers
}
