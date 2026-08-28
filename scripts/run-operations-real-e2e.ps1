param(
    [string]$TomcatHome = 'C:\Program Files\Apache Software Foundation\Tomcat 11.0',
    [string]$TempRoot = 'C:\Users\NamPhong\AppData\Local\Temp\opencode\fastguy-operations-e2e',
    [int]$BackendPort = 18080,
    [int]$ShutdownPort = 18005,
    [int]$FrontendPort = 15174,
    [switch]$SafetySelfTest
)

if ($env:FASTGUY_DISPOSABLE_DB -ne 'true') { throw 'FASTGUY_DISPOSABLE_DB=true required' }
foreach ($name in 'DB_URL','DB_USER','DB_PASSWORD') {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) { throw "$name required" }
}
if ($env:DB_URL -notmatch '(^|[;=/])FastGuyDB_Operations060_Test([;?&]|$)') { throw 'DB_URL must target FastGuyDB_Operations060_Test' }

$script = Join-Path $PSScriptRoot 'run-staff-dispatch-real-e2e.ps1'
& $script -TomcatHome $TomcatHome -TempRoot $TempRoot -BackendPort $BackendPort -ShutdownPort $ShutdownPort -FrontendPort $FrontendPort -Operations -Project desktop-chrome -SafetySelfTest:$SafetySelfTest
if ($LASTEXITCODE -ne 0 -or $SafetySelfTest) { exit $LASTEXITCODE }

$env:PLAYWRIGHT_API_TARGET = 'http://127.0.0.1:1/FastGuy'
Push-Location (Join-Path (Split-Path -Parent $PSScriptRoot) 'frontend')
try {
    & npx.cmd playwright test tests/e2e/shipper-field-command.spec.js --project=desktop-chrome --project=mobile-chrome --config=playwright.real-backend.config.js
    if ($LASTEXITCODE -ne 0) { throw 'Shipper desktop/mobile Playwright failed' }
} finally {
    Pop-Location
}
