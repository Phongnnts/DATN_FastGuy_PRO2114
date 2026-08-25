param(
    [string]$TomcatHome = 'C:\Program Files\Apache Software Foundation\Tomcat 11.0',
    [string]$TempRoot = 'C:\Users\NamPhong\AppData\Local\Temp\opencode\fastguy-staff-e2e',
    [int]$BackendPort = 18080,
    [int]$ShutdownPort = 18005,
    [int]$FrontendPort = 15174
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repo 'Backend\FastGuy-FastFoodSite'
$frontend = Join-Path $repo 'frontend'
$catalinaBase = Join-Path $TempRoot 'tomcat-base'
$backendProcess = $null
$primaryFailure = $null
$secondaryFailures = [System.Collections.Generic.List[string]]::new()

function New-RandomSecret([int]$Bytes) {
    $buffer = New-Object byte[] $Bytes
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($buffer) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($buffer)
}

function Invoke-Fixture([string]$Action, [string]$RunId) {
    $env:FASTGUY_E2E_RUN_ID = $RunId
    & mvn.cmd '-Dtest=integration.StaffDispatchBrowserFixtureIT' "-De2e.action=$Action" test -f (Join-Path $backend 'pom.xml')
    if ($LASTEXITCODE -ne 0) { throw "Fixture $Action failed" }
}

function Wait-FastGuy([string]$Url, [int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2
            $listener = Get-NetTCPConnection -State Listen -LocalPort $BackendPort -ErrorAction SilentlyContinue
            $owner = if ($listener) { Get-CimInstance Win32_Process -Filter "ProcessId=$($listener[0].OwningProcess)" }
            $payload = $response.Content | ConvertFrom-Json
            if ($response.StatusCode -eq 200 -and $payload.status -eq 'success' -and $payload.data.storeName -eq 'FastGuy' -and $owner.CommandLine -like "*$catalinaBase*") { return }
        } catch {}
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for $Url"
}

if ($env:FASTGUY_DISPOSABLE_DB -ne 'true') { throw 'FASTGUY_DISPOSABLE_DB=true required' }
foreach ($name in 'DB_URL','DB_USER','DB_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) { throw "$name required" }
}
if (-not (Test-Path -LiteralPath (Join-Path $TomcatHome 'bin\catalina.bat'))) { throw 'Installed Tomcat 11 not found' }
if ((Get-Service -Name Tomcat11).Status -ne 'Stopped') { throw 'Tomcat11 Windows service must remain stopped' }
if (-not (Test-Path -LiteralPath (Split-Path -Parent $TempRoot))) { throw 'Approved temp parent missing' }
foreach ($port in $BackendPort,$ShutdownPort,$FrontendPort) {
    if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "Harness port $port occupied before startup" }
}

$env:FASTGUY_E2E_DB_NAME = 'FastGuyDB_Inventory054_Test'
$env:FASTGUY_E2E_STAFF_PASSWORD = New-RandomSecret 24
$env:FASTGUY_E2E_BACKEND_DIR = $backend
$env:FASTGUY_E2E_MAVEN_HOME = Split-Path (Split-Path (Get-Command mvn.cmd).Source -Parent) -Parent
$env:JWT_SECRET = New-RandomSecret 48

try {
    if (Test-Path -LiteralPath $TempRoot) { Remove-Item -LiteralPath $TempRoot -Recurse -Force }
    New-Item -ItemType Directory -Path (Join-Path $catalinaBase 'conf') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $TomcatHome 'conf\web.xml') -Destination (Join-Path $catalinaBase 'conf\web.xml')
    foreach ($directory in 'logs','temp','webapps','work') {
        New-Item -ItemType Directory -Path (Join-Path $catalinaBase $directory) | Out-Null
    }
    $serverXml = Join-Path $catalinaBase 'conf\server.xml'
    @"
<Server port="$ShutdownPort" shutdown="SHUTDOWN">
  <Service name="Catalina">
    <Connector port="$BackendPort" protocol="HTTP/1.1" address="127.0.0.1" connectionTimeout="20000" />
    <Engine name="Catalina" defaultHost="localhost">
      <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true" />
    </Engine>
  </Service>
</Server>
"@ | Set-Content -LiteralPath $serverXml -Encoding UTF8

    & mvn.cmd package -DskipTests -f (Join-Path $backend 'pom.xml')
    if ($LASTEXITCODE -ne 0) { throw 'WAR build failed' }
    Copy-Item -LiteralPath (Join-Path $backend 'target\FastGuy.war') -Destination (Join-Path $catalinaBase 'webapps\FastGuy.war')

    $env:CATALINA_HOME = $TomcatHome
    $env:CATALINA_BASE = $catalinaBase
    $backendLog = Join-Path $TempRoot 'tomcat.stdout.log'
    $backendError = Join-Path $TempRoot 'tomcat.stderr.log'
    $backendProcess = Start-Process -FilePath (Join-Path $TomcatHome 'bin\catalina.bat') -ArgumentList 'run' -PassThru -NoNewWindow -RedirectStandardOutput $backendLog -RedirectStandardError $backendError
    Wait-FastGuy "http://127.0.0.1:$BackendPort/FastGuy/api/store/config" 120
    $runtimePids = @(Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*$catalinaBase*" } | ForEach-Object ProcessId)
    $runtimePorts = @(Get-NetTCPConnection -State Listen | Where-Object { $runtimePids -contains $_.OwningProcess } | ForEach-Object LocalPort | Sort-Object -Unique)
    $expectedPorts = @($ShutdownPort,$BackendPort) | Sort-Object
    if (Compare-Object $runtimePorts $expectedPorts) { throw "Unexpected isolated Tomcat listeners: $runtimePorts" }

    foreach ($project in 'desktop-chrome','mobile-chrome') {
        $runId = ((Get-Date).ToString('yyyyMMddHHmmssfff') + $project.Substring(0,1)).ToLower()
        $env:FASTGUY_E2E_RUN_ID = $runId
        $env:FASTGUY_E2E_STAFF_EMAIL = "staff-$runId@test.local"
        $projectFailure = $null
        try {
            Invoke-Fixture 'seed' $runId
            $env:PLAYWRIGHT_API_TARGET = "http://127.0.0.1:$BackendPort/FastGuy"
            $env:PLAYWRIGHT_BASE_URL = ''
            Push-Location $frontend
            try {
                & npx.cmd playwright test tests/e2e/staff-dispatch-real-backend.spec.js "--project=$project" --config=playwright.real-backend.config.js
                if ($LASTEXITCODE -ne 0) { throw "Playwright $project failed" }
            } finally {
                Pop-Location
            }
        } catch {
            $projectFailure = $_
        } finally {
            try { Invoke-Fixture 'cleanup' $runId } catch { $secondaryFailures.Add("$project cleanup: $($_.Exception.Message)") }
        }
        if ($projectFailure) { throw $projectFailure }
    }
} catch {
    $primaryFailure = $_
} finally {
    try { if ($backendProcess -and -not $backendProcess.HasExited) { Stop-Process -Id $backendProcess.Id -Force; $backendProcess.WaitForExit(10000) | Out-Null } } catch { $secondaryFailures.Add("backend stop: $($_.Exception.Message)") }
    try { Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($catalinaBase) } | ForEach-Object { taskkill.exe /PID $_.ProcessId /T /F | Out-Null }; Start-Sleep -Milliseconds 500 } catch { $secondaryFailures.Add("process cleanup: $($_.Exception.Message)") }
    foreach ($port in $BackendPort,$ShutdownPort,$FrontendPort) { try { if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "Harness port $port still listening" } } catch { $secondaryFailures.Add($_.Exception.Message) } }
    Remove-Item Env:FASTGUY_E2E_STAFF_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
    try { if ((Get-Service -Name Tomcat11).Status -ne 'Stopped') { throw 'Tomcat11 Windows service state changed unexpectedly' } } catch { $secondaryFailures.Add($_.Exception.Message) }
}
if ($secondaryFailures.Count) { [Console]::Error.WriteLine("Secondary cleanup failures:`n" + ($secondaryFailures -join "`n")) }
if ($primaryFailure) { throw $primaryFailure }
if ($secondaryFailures.Count) { throw ($secondaryFailures -join '; ') }
