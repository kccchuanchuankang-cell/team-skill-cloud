<#
.SYNOPSIS
  Scaffold a new skill under skills/<name>/ from the template (then you edit and open a PR).

.PARAMETER Name
  Folder id: lowercase, digits, hyphens only (e.g. my-skill).

.EXAMPLE
  .\scripts\new-skill.ps1 -Name data-pipeline
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9]+(-[a-z0-9]+)*$')]
    [string]$Name
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dest = Join-Path (Join-Path $root "skills") $Name
$templateDir = Join-Path $root "templates\skill-template"
$templateSkill = Join-Path $templateDir "SKILL.md"

if (Test-Path $dest) {
    Write-Error "Already exists: $dest"
}

New-Item -ItemType Directory -Path $dest | Out-Null

$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$skillMd = Get-Content -LiteralPath $templateSkill -Raw -Encoding UTF8
$skillMd = $skillMd -replace '(?m)^name:\s*example-skill\s*$', "name: $Name"
$skillPath = Join-Path $dest "SKILL.md"
[System.IO.File]::WriteAllText($skillPath, $skillMd, $utf8NoBom)

function ConvertTo-TitleCaseSegment([string]$s) {
    if ([string]::IsNullOrEmpty($s)) { return "" }
    if ($s.Length -eq 1) { return $s.ToUpperInvariant() }
    return $s.Substring(0, 1).ToUpperInvariant() + $s.Substring(1)
}

$title = ($Name -split '-' | ForEach-Object { ConvertTo-TitleCaseSegment $_ }) -join ' '
$meta = [ordered]@{
    name          = $Name
    title         = $title
    description   = "TODO: one-line catalog description"
    tags          = @("todo")
    owner         = "your-team"
    status        = "draft"
    version       = "0.1.0"
    summary       = "TODO: detail summary (when to use this skill)"
    use_cases     = @("TODO: use case 1", "TODO: use case 2")
    install_hint  = "TODO: which product repos should install this skill"
}
$metaJson = $meta | ConvertTo-Json -Depth 6
$metaPath = Join-Path $dest "meta.json"
[System.IO.File]::WriteAllText($metaPath, $metaJson, $utf8NoBom)

Write-Host "Created $dest"
Write-Host ""
Write-Host "Next:"
Write-Host "  1. Edit SKILL.md, meta.json, add references/ or scripts/ as needed"
Write-Host "  2. python scripts/validate-skills.py"
Write-Host "  3. powershell -ExecutionPolicy Bypass -File .\scripts\generate-catalog.ps1"
Write-Host "  4. git checkout -b skill/$Name"
Write-Host "  5. git add skills/$Name catalog/data"
Write-Host ('  6. git commit -m "Add skill: ' + $Name + '"')
Write-Host "  7. git push -u origin skill/$Name   (open PR on GitHub)"
