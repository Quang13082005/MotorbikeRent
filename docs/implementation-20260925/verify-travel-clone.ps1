$ErrorActionPreference = 'Stop'
# This server must be started with the isolated verification database and dummy integrations.
$base = 'http://127.0.0.1:8082'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$null = Invoke-WebRequest "$base/auth/login" -WebSession $session -UseBasicParsing
function Csrf { return $session.Cookies.GetCookies([uri]$base)['XSRF-TOKEN'].Value }
$login = Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $session -Headers @{'X-XSRF-TOKEN'=(Csrf)} -ContentType 'application/json' -Body '{"username":"admin_demo@motorent.vn","password":"admin123"}'
$headers = @{Authorization="Bearer $($login.result.token)"}
if (!$login.result.token) { throw 'Login failed' }
$null = Invoke-WebRequest "$base/admin/travel-places" -WebSession $session -Headers $headers -UseBasicParsing
$headers['X-XSRF-TOKEN'] = Csrf
$endpoint = "$base/api/operations/data/travel-places"
$name = 'DEMO kiểm thử địa điểm 20260927'
$payload = @{name=$name;address='Đà Nẵng';published='false';lat='16.06';lng='108.22'}
function SavePlace($url,$method) {
  $bytes=[Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json))
  $null=Invoke-RestMethod $url -Method $method -WebSession $session -Headers $headers -ContentType 'application/json; charset=utf-8' -Body $bytes
}
try {
 $list=Invoke-RestMethod ($endpoint+'?query='+[uri]::EscapeDataString($name)) -WebSession $session -Headers $headers
 $place=@($list.items | Where-Object name -eq $name) | Select-Object -First 1
 if (!$place) { SavePlace $endpoint Post }
 $list=Invoke-RestMethod ($endpoint+'?query='+[uri]::EscapeDataString($name)) -WebSession $session -Headers $headers
 $place=@($list.items | Where-Object name -eq $name) | Select-Object -First 1
 if (!$place) { throw 'Record was not persisted' }
 $url="$endpoint/$($place.id)"
 SavePlace $url Put
 $query=[uri]::EscapeDataString($name)
 $page=Invoke-WebRequest "$base/customer/travel-places?query=$query" -UseBasicParsing
 if ($page.Content.Contains('<h2>'+$name+'</h2>')) { throw 'Draft is public' }
 $payload.published='true'; SavePlace $url Put
 $page=Invoke-WebRequest "$base/customer/travel-places?query=$query" -UseBasicParsing
 if (!$page.Content.Contains('<h2>'+$name+'</h2>')) { throw 'Published record missing' }
 $payload.published='false'; SavePlace $url Put
 Write-Output 'PASS: MySQL create/read/update, Vietnamese text, publish and hide'
} finally {
 if ($url) { $payload.published='false'; SavePlace $url Put }
 $null=Invoke-RestMethod "$base/api/auth/logout" -Method Post -WebSession $session -Headers $headers
}
