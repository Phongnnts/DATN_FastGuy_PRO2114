param(
    [string]$TomcatHome = 'C:\Program Files\Apache Software Foundation\Tomcat 11.0',
    [string]$TempRoot = 'C:\Users\NamPhong\AppData\Local\Temp\opencode\fastguy-staff-e2e',
    [int]$BackendPort = 18080,
    [int]$ShutdownPort = 18005,
    [int]$AjpPort = 18009,
    [int]$FrontendPort = 15174
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $repo 'Backend\FastGuy-FastFoodSite'
$frontend = Join-Path $repo 'frontend'
$catalinaBase = Join-Path $TempRoot 'tomcat-base'
$backendProcess = $null

function New-RandomSecret([int]$Bytes) {
    return [Convert]::ToBase64String((1..$Bytes | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
}

function Invoke-Fixture([string]$Action, [string]$RunId) {
    $env:FASTGUY_E2E_RUN_ID = $RunId
    & mvn.cmd '-Dtest=integration.StaffDispatchBrowserFixtureIT' "-De2e.action=$Action" test -f (Join-Path $backend 'pom.xml')
    if ($LASTEXITCODE -ne 0) { throw "Fixture $Action failed" }
}

function Wait-Http([string]$Url, [int]$Seconds) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2
            if ($response.StatusCode -lt 500) { return }
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

$env:FASTGUY_E2E_DB_NAME = 'FastGuyDB_Inventory054_Test'
$env:FASTGUY_E2E_STAFF_PASSWORD = New-RandomSecret 24
$env:FASTGUY_E2E_BACKEND_DIR = $backend
$env:JWT_SECRET = New-RandomSecret 48

try {
    if (Test-Path -LiteralPath $TempRoot) { Remove-Item -LiteralPath $TempRoot -Recurse -Force }
    New-Item -ItemType Directory -Path $catalinaBase | Out-Null
    Copy-Item -LiteralPath (Join-Path $TomcatHome 'conf') -Destination $catalinaBase -Recurse
    foreach ($directory in 'logs','temp','webapps','work') {
        New-Item -ItemType Directory -Path (Join-Path $catalinaBase $directory) | Out-Null
    }
    $serverXml = Join-Path $catalinaBase 'conf\server.xml'
    [xml]$xml = [IO.File]::ReadAllText($serverXml)
    $xml.Server.SetAttribute('port', [string]$ShutdownPort)
    $httpConnector = @($xml.Server.Service.Connector | Where-Object { $_.protocol -eq 'HTTP/1.1' })
    if ($httpConnector.Count -ne 1) { throw 'Expected exactly one active HTTP connector' }
    $httpConnector[0].SetAttribute('port', [string]$BackendPort)
    $xml.Save($serverXml)

    & mvn.cmd package -DskipTests -f (Join-Path $backend 'pom.xml')
    if ($LASTEXITCODE -ne 0) { throw 'WAR build failed' }
    Copy-Item -LiteralPath (Join-Path $backend 'target\FastGuy.war') -Destination (Join-Path $catalinaBase 'webapps\FastGuy.war')

    $env:CATALINA_HOME = $TomcatHome
    $env:CATALINA_BASE = $catalinaBase
    $backendLog = Join-Path $TempRoot 'tomcat.stdout.log'
    $backendError = Join-Path $TempRoot 'tomcat.stderr.log'
    $backendProcess = Start-Process -FilePath (Join-Path $TomcatHome 'bin\catalina.bat') -ArgumentList 'run' -PassThru -NoNewWindow -RedirectStandardOutput $backendLog -RedirectStandardError $backendError
    Wait-Http "http://127.0.0.1:$BackendPort/FastGuy/api/store/config" 120

    foreach ($project in 'desktop-chrome','mobile-chrome') {
        $runId = ((Get-Date).ToString('yyyyMMddHHmmssfff') + $project.Substring(0,1)).ToLower()
        $env:FASTGUY_E2E_RUN_ID = $runId
        $env:FASTGUY_E2E_STAFF_EMAIL = "staff-$runId@test.local"
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
        } finally {
            Invoke-Fixture 'cleanup' $runId
        }
    }
} finally {
    if ($backendProcess -and -not $backendProcess.HasExited) {
        Stop-Process -Id $backendProcess.Id -Force
        $backendProcess.WaitForExit(10000)
    }
    Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*$catalinaBase*" } | ForEach-Object {
        taskkill.exe /PID $_.ProcessId /T /F | Out-Null
    }
    foreach ($port in $BackendPort,$ShutdownPort,$FrontendPort) {
        if (Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue) { throw "Harness port $port still listening" }
    }
    Remove-Item Env:FASTGUY_E2E_STAFF_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
    if ((Get-Service -Name Tomcat11).Status -ne 'Stopped') { throw 'Tomcat11 Windows service state changed unexpectedly' }
}
