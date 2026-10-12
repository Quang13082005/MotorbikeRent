$ErrorActionPreference='Stop'
$projectRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$jar=Join-Path $projectRoot 'target/phase6/motorbike-rental-0.0.1-SNAPSHOT.jar'
if(!(Test-Path -LiteralPath $jar)){throw 'Build the verified application JAR before starting.'}
$port=8085
if(Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue){throw "Port $port is already in use; no process was stopped."}
$logDir=Join-Path $projectRoot 'target/runtime'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $projectRoot 'target/sockets') -Force | Out-Null
$javaArgs=@(('"-Djdk.net.unixdomain.tmpdir='+$projectRoot+'/target/sockets"'),'-jar',('"'+$jar+'"'),'--server.address=127.0.0.1',"--server.port=$port",'--spring.jpa.hibernate.ddl-auto=validate','--app.seed-demo=false',"--app.base-url=http://127.0.0.1:$port")
$javaArgs+=@('--spring.datasource.url=jdbc:mysql://localhost:3306/motorent_verify_20260926?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh','--app.payments-enabled=false','--payos.client-id=test-placeholder','--payos.api-key=test-placeholder','--payos.checksum-key=test-placeholder','--spring.ai.openai.api-key=test','--google.cloud.vision.api-key=test','--google.maps.api-key=','--spring.mail.host=127.0.0.1','--spring.mail.port=1')
$javaArgs+="--logging.file.name=target/runtime/application-$port.log"
$process=Start-Process -FilePath (Get-Command java).Source -ArgumentList $javaArgs -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir "stdout-$port.log") -RedirectStandardError (Join-Path $logDir "stderr-$port.log") -PassThru
Write-Output "Started process $($process.Id). Waiting for http://127.0.0.1:$port/customer/home"
for($attempt=0;$attempt-lt 45;$attempt++){
 if($process.HasExited){throw "Application exited; inspect target/runtime/stdout-$port.log and stderr-$port.log"}
 try{$response=Invoke-WebRequest "http://127.0.0.1:$port/customer/home" -UseBasicParsing -TimeoutSec 2;if($response.StatusCode-eq 200){Write-Output "READY http://127.0.0.1:$port/customer/home";exit 0}}catch{}
 Start-Sleep -Seconds 1
}
throw "Server is still starting; inspect target/runtime/application-$port.log"
