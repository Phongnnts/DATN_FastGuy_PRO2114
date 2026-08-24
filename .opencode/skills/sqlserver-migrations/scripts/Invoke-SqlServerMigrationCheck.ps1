[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [ValidateSet('Preflight', 'Validate')]
    [string]$Mode,

    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$Server,

    [Parameter(Mandatory)]
    [ValidatePattern('^[A-Za-z0-9_\-]+$')]
    [string]$Database,

    [string]$ScriptPath,

    [switch]$Disposable,

    [switch]$SelfCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Assert-SafeValidator {
    param([Parameter(Mandatory)][string]$Path)

    $resolved = (Resolve-Path -LiteralPath $Path).Path
    $workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..\..\..\..')).Path
    $migrations = Join-Path $workspace 'database\migrations'

    if (-not $resolved.StartsWith($migrations, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Validator must be inside database\migrations.'
    }

    if ([System.IO.Path]::GetFileName($resolved) -notmatch '(?i)validate') {
        throw 'Validate mode accepts validator scripts only.'
    }

    $sql = [System.IO.File]::ReadAllText($resolved)
    if ($sql -match '(?im)\b(DROP\s+DATABASE|TRUNCATE\s+TABLE|DROP\s+TABLE|DELETE\s+FROM|UPDATE\s+|INSERT\s+INTO|MERGE\s+)') {
        throw 'Validator contains a destructive or mutating statement.'
    }

    return $resolved
}

function Invoke-SqlCmd {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $command = Get-Command sqlcmd -ErrorAction Stop
    & $command.Source @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "sqlcmd failed with exit code $LASTEXITCODE."
    }
}

if ($SelfCheck) {
    if ($Mode -ne 'Preflight') {
        throw 'SelfCheck uses Preflight mode.'
    }
    if ('FastGuyDB' -notmatch '^[A-Za-z0-9_\-]+$') {
        throw 'Database validation self-check failed.'
    }
    if ('FastGuyDB;DROP DATABASE master' -match '^[A-Za-z0-9_\-]+$') {
        throw 'Unsafe database name passed self-check.'
    }
    'Self-check passed.'
    exit 0
}

if ($Mode -eq 'Validate' -and -not $Disposable) {
    throw 'Validate mode requires -Disposable. Retained databases need explicit manual approval outside this wrapper.'
}

$identityQuery = 'SET NOCOUNT ON; SELECT @@SERVERNAME AS ServerName, DB_NAME() AS DatabaseName, state_desc AS DatabaseState, compatibility_level AS CompatibilityLevel FROM sys.databases WHERE name = DB_NAME();'
Invoke-SqlCmd -Arguments @('-S', $Server, '-d', $Database, '-E', '-b', '-V', '16', '-f', '65001', '-Q', $identityQuery)

if ($Mode -eq 'Validate') {
    if ([string]::IsNullOrWhiteSpace($ScriptPath)) {
        throw 'Validate mode requires -ScriptPath.'
    }
    $validator = Assert-SafeValidator -Path $ScriptPath
    Invoke-SqlCmd -Arguments @('-S', $Server, '-d', $Database, '-E', '-b', '-V', '16', '-f', '65001', '-i', $validator)
}
