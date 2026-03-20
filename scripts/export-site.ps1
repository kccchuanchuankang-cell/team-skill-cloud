$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$catalogRoot = Join-Path $root "catalog"
$distRoot = Join-Path $root "dist"
$siteRoot = Join-Path $distRoot "site"

& (Join-Path $PSScriptRoot "generate-catalog.ps1")

if (Test-Path $siteRoot) {
    Remove-Item -Path $siteRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $siteRoot | Out-Null
Copy-Item -Path (Join-Path $catalogRoot "*") -Destination $siteRoot -Recurse -Force
Set-Content -Path (Join-Path $siteRoot ".nojekyll") -Value "" -Encoding ASCII

Write-Host "Exported static site to $siteRoot"
