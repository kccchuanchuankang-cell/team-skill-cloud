param(
    [string]$OutputRoot = "",
    [string]$BaseUrl = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $root "skills"

if ([string]::IsNullOrWhiteSpace($OutputRoot)) {
    $OutputRoot = Join-Path $root "dist\registry"
}

$registryRoot = $OutputRoot
$skillsOutRoot = Join-Path $registryRoot "skills"

if (Test-Path $registryRoot) {
    Remove-Item -Recurse -Force $registryRoot
}

New-Item -ItemType Directory -Path $skillsOutRoot -Force | Out-Null

$publishedAt = [DateTime]::UtcNow.ToString("o")
$skillSummaries = @()

function Join-RegistryUrl {
    param(
        [string]$MaybeBase,
        [string]$RelativePath
    )

    $normalizedPath = ($RelativePath -replace '\\', '/').TrimStart('/')
    if ([string]::IsNullOrWhiteSpace($MaybeBase)) {
        return "/" + $normalizedPath
    }

    return ($MaybeBase.TrimEnd('/') + "/" + $normalizedPath)
}

Get-ChildItem -Path $skillsRoot -Directory | Sort-Object Name | ForEach-Object {
    $skillId = $_.Name
    $metaPath = Join-Path $_.FullName "meta.json"
    $meta = Get-Content -LiteralPath $metaPath -Raw -Encoding UTF8 | ConvertFrom-Json

    $packageInfo = & (Join-Path $PSScriptRoot "package-skill.ps1") -SkillId $skillId -OutputRoot $registryRoot
    $version = [string]$packageInfo.version

    $skillIndexRel = ("skills/{0}/index.json" -f $skillId)
    $packageRel = ("skills/{0}/versions/{1}.zip" -f $skillId, $version)
    $notesRel = "CHANGELOG.md"

    $versionRecord = [ordered]@{
        version = $version
        published_at = $publishedAt
        breaking = $false
        manifest_version = "1"
        package_url = Join-RegistryUrl -MaybeBase $BaseUrl -RelativePath $packageRel
        checksum_sha256 = [string]$packageInfo.checksum_sha256
        notes_url = Join-RegistryUrl -MaybeBase $BaseUrl -RelativePath $notesRel
    }

    $perSkillIndex = [ordered]@{
        id = $meta.name
        title = $meta.title
        description = $meta.description
        owner = $meta.owner
        versions = @($versionRecord)
    }

    $perSkillIndexPath = Join-Path $skillsOutRoot ("{0}\index.json" -f $skillId)
    New-Item -ItemType Directory -Path (Split-Path -Parent $perSkillIndexPath) -Force | Out-Null
    Set-Content -Path $perSkillIndexPath -Value ($perSkillIndex | ConvertTo-Json -Depth 8) -Encoding UTF8

    $skillSummaries += [pscustomobject]([ordered]@{
        id = $meta.name
        title = $meta.title
        description = $meta.description
        owner = $meta.owner
        tags = @($meta.tags)
        latest_version = $version
        latest_stable_version = $version
        index_url = Join-RegistryUrl -MaybeBase $BaseUrl -RelativePath $skillIndexRel
    })
}

$rootIndex = [ordered]@{
    registry_version = "1"
    generated_at = $publishedAt
    skills = @($skillSummaries)
}

$rootIndexPath = Join-Path $registryRoot "index.json"
Set-Content -Path $rootIndexPath -Value ($rootIndex | ConvertTo-Json -Depth 8) -Encoding UTF8

Write-Host "Built registry output:"
Write-Host " - $rootIndexPath"
Write-Host " - $skillsOutRoot"
