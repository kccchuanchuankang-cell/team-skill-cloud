<#
.SYNOPSIS
  Regenerate catalog/data when files under skills/ change (local dev).

.DESCRIPTION
  The catalog UI reads generated catalog/data/skills.js — it does not scan the repo at runtime.
  This script runs generate-catalog.ps1 whenever something under skills/ changes; refresh the browser to see updates.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\scripts\watch-catalog.ps1
#>
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $root "skills"
$generate = Join-Path $PSScriptRoot "generate-catalog.ps1"

if (-not (Test-Path $skillsRoot)) {
    Write-Error "skills/ not found at $skillsRoot"
}

$debounceMs = 600
$burstWaitMs = 150

Write-Host "Watching $skillsRoot (Ctrl+C to stop). Refresh catalog/index.html after each regen."
& $generate

$watcher = New-Object System.IO.FileSystemWatcher
$watcher.Path = $skillsRoot
$watcher.IncludeSubdirectories = $true
$watcher.Filter = "*.*"
$watcher.NotifyFilter = [System.IO.NotifyFilters]::FileName -bor [System.IO.NotifyFilters]::LastWrite -bor [System.IO.NotifyFilters]::DirectoryName
$watcher.EnableRaisingEvents = $true

try {
    while ($true) {
        $null = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, -1)
        Start-Sleep -Milliseconds $debounceMs
        do {
            $r = $watcher.WaitForChanged([System.IO.WatcherChangeTypes]::All, $burstWaitMs)
        } while (-not $r.TimedOut)

        Write-Host ("[{0}] Regenerating catalog..." -f (Get-Date -Format "HH:mm:ss"))
        & $generate
    }
}
finally {
    $watcher.EnableRaisingEvents = $false
    $watcher.Dispose()
}
