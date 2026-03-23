param(
    [Parameter(Mandatory = $true)]
    [string]$SkillId,

    [string]$OutputRoot = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $root "skills"
$skillRoot = Join-Path $skillsRoot $SkillId

if (-not (Test-Path $skillRoot)) {
    throw "Skill not found: $SkillId"
}

$metaPath = Join-Path $skillRoot "meta.json"
if (-not (Test-Path $metaPath)) {
    throw "meta.json not found for skill: $SkillId"
}

$meta = Get-Content -LiteralPath $metaPath -Raw -Encoding UTF8 | ConvertFrom-Json
$version = [string]$meta.version
if ([string]::IsNullOrWhiteSpace($version)) {
    throw "meta.json version is required for skill: $SkillId"
}

if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $root "dist\registry"
}

$versionsRoot = Join-Path $OutputRoot ("skills\{0}\versions" -f $SkillId)
$stagingRoot = Join-Path $OutputRoot ".staging"
$stagingDir = Join-Path $stagingRoot ("{0}-{1}" -f $SkillId, $version)
$stagingSkillDir = Join-Path $stagingDir $SkillId
$zipPath = Join-Path $versionsRoot ("{0}.zip" -f $version)
$shaPath = Join-Path $versionsRoot ("{0}.sha256" -f $version)

New-Item -ItemType Directory -Path $versionsRoot -Force | Out-Null

if (Test-Path $stagingDir) {
    Remove-Item -Recurse -Force $stagingDir
}

New-Item -ItemType Directory -Path $stagingSkillDir -Force | Out-Null
Copy-Item -Path (Join-Path $skillRoot "*") -Destination $stagingSkillDir -Recurse -Force

if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}

Compress-Archive -Path (Join-Path $stagingDir "*") -DestinationPath $zipPath -CompressionLevel Optimal

$hash = (Get-FileHash -Algorithm SHA256 -Path $zipPath).Hash.ToLowerInvariant()
Set-Content -Path $shaPath -Value $hash -Encoding ASCII

Remove-Item -Recurse -Force $stagingDir

if ((Test-Path $stagingRoot) -and -not (Get-ChildItem -Force $stagingRoot | Select-Object -First 1)) {
    Remove-Item -Force $stagingRoot
}

[pscustomobject]@{
    skill_id = $SkillId
    version = $version
    zip_path = $zipPath
    checksum_sha256 = $hash
    checksum_path = $shaPath
}
