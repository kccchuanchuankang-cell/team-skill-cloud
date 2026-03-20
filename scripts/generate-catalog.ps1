$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$skillsRoot = Join-Path $root "skills"
$catalogDataRoot = Join-Path $root "catalog\data"
$catalogConfigPath = Join-Path $root "catalog\catalog-config.json"
$jsonOut = Join-Path $catalogDataRoot "skills.json"
$jsOut = Join-Path $catalogDataRoot "skills.js"

function Get-GitSourceRef {
    param([string]$RepoRoot)
    $prevEa = $ErrorActionPreference
    try {
        $ErrorActionPreference = "SilentlyContinue"
        Push-Location $RepoRoot
        try {
            $ref = git rev-parse --abbrev-ref HEAD
            if ($LASTEXITCODE -eq 0 -and $ref -and ($ref.Trim() -ne "HEAD")) {
                return $ref.Trim()
            }
            $sha = git rev-parse HEAD
            if ($LASTEXITCODE -eq 0 -and $sha) {
                return $sha.Trim()
            }
            return "main"
        }
        finally {
            Pop-Location
        }
    }
    finally {
        $ErrorActionPreference = $prevEa
    }
}

function Convert-GitRemoteMetadata {
    param([string]$Remote)
    $r = $Remote.Trim()
    if ([string]::IsNullOrWhiteSpace($r)) {
        return $null
    }

    if ($r -match '^git@github\.com:(.+)$') {
        $pathPart = ($Matches[1] -replace '\.git$', '').Trim()
        return [pscustomobject]@{
            Provider = "github"
            Ssh      = if ($r -match '\.git$') { $r } else { "git@github.com:$pathPart.git" }
            Https    = "https://github.com/$pathPart.git"
            WebBase  = "https://github.com/$pathPart"
        }
    }

    if ($r -match '^https://github\.com/(.+)$') {
        $pathPart = $Matches[1] -replace '\.git$', '' -replace '/$', ''
        return [pscustomobject]@{
            Provider = "github"
            Ssh      = "git@github.com:$pathPart.git"
            Https    = "https://github.com/$pathPart.git"
            WebBase  = "https://github.com/$pathPart"
        }
    }

    if ($r -match '^git@gitlab\.com:(.+)$') {
        $pathPart = ($Matches[1] -replace '\.git$', '').Trim()
        return [pscustomobject]@{
            Provider = "gitlab"
            Ssh      = if ($r -match '\.git$') { $r } else { "git@gitlab.com:$pathPart.git" }
            Https    = "https://gitlab.com/$pathPart.git"
            WebBase  = "https://gitlab.com/$pathPart"
        }
    }

    if ($r -match '^https://gitlab\.com/(.+)$') {
        $pathPart = $Matches[1] -replace '\.git$', '' -replace '/$', ''
        return [pscustomobject]@{
            Provider = "gitlab"
            Ssh      = "git@gitlab.com:$pathPart.git"
            Https    = "https://gitlab.com/$pathPart.git"
            WebBase  = "https://gitlab.com/$pathPart"
        }
    }

    if ($r -match '^https?://') {
        return [pscustomobject]@{
            Provider = "generic"
            Ssh      = $null
            Https    = $r
            WebBase  = $null
        }
    }

    return [pscustomobject]@{
        Provider = "unknown"
        Ssh      = $r
        Https    = $null
        WebBase  = $null
    }
}

function Resolve-SourceBrowserUrl {
    param(
        [string]$Provider,
        [string]$WebBase,
        [string]$Ref,
        [string]$RelPath
    )
    if ([string]::IsNullOrWhiteSpace($WebBase) -or [string]::IsNullOrWhiteSpace($Ref) -or [string]::IsNullOrWhiteSpace($RelPath)) {
        return $null
    }
    $rp = $RelPath -replace '\\', '/'
    switch ($Provider) {
        "github" { return "$WebBase/blob/$Ref/$rp" }
        "gitlab" { return "$WebBase/-/blob/$Ref/$rp" }
        default { return $null }
    }
}

$defaultConfig = [ordered]@{
    skills_repo_ssh   = "git@github.com:your-org/skills-cloud.git"
    skills_repo_https = "https://github.com/your-org/skills-cloud.git"
    config_note       = "Set git remote origin, or edit catalog/catalog-config.json to override URLs."
    repo_web_base     = ""
    source_ref        = ""
    remote_provider   = ""
    remote_detected   = $false
}

$catalogConfig = [ordered]@{}
foreach ($key in $defaultConfig.Keys) {
    $catalogConfig[$key] = $defaultConfig[$key]
}

$fileConfig = $null
if (Test-Path $catalogConfigPath) {
    $fileConfig = Get-Content -LiteralPath $catalogConfigPath -Raw -Encoding UTF8 | ConvertFrom-Json
}

$disableAuto = $false
if ($null -ne $fileConfig -and $fileConfig.PSObject.Properties["disable_git_origin"] -and ($fileConfig.disable_git_origin -eq $true)) {
    $disableAuto = $true
}

if (-not $disableAuto -and (Test-Path (Join-Path $root ".git"))) {
    $prevEa = $ErrorActionPreference
    try {
        $ErrorActionPreference = "SilentlyContinue"
        Push-Location $root
        try {
            $inGit = git rev-parse --is-inside-work-tree 2>$null
            if ($LASTEXITCODE -eq 0 -and $inGit -eq "true") {
                $remote = git remote get-url origin 2>$null
                if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($remote)) {
                    $meta = Convert-GitRemoteMetadata -Remote $remote
                    if ($null -ne $meta) {
                        if ($meta.Ssh) {
                            $catalogConfig.skills_repo_ssh = $meta.Ssh
                        }
                        if ($meta.Https) {
                            $catalogConfig.skills_repo_https = $meta.Https
                        }
                        if ($meta.WebBase) {
                            $catalogConfig.repo_web_base = $meta.WebBase
                        }
                        $catalogConfig.remote_provider = $meta.Provider
                        $catalogConfig.source_ref = Get-GitSourceRef -RepoRoot $root
                        $catalogConfig.remote_detected = $true
                        Write-Host "Detected git origin -> clone URLs and source_ref=$($catalogConfig.source_ref) (provider=$($meta.Provider))"
                    }
                }
            }
        }
        finally {
            Pop-Location
        }
    }
    finally {
        $ErrorActionPreference = $prevEa
    }
}

if ($null -ne $fileConfig) {
    foreach ($prop in $fileConfig.PSObject.Properties) {
        if ($prop.Name -in @("disable_git_origin")) {
            continue
        }
        $value = $prop.Value
        if ($null -eq $value) {
            continue
        }
        if ($value -is [string] -and [string]::IsNullOrWhiteSpace($value)) {
            continue
        }
        $catalogConfig[$prop.Name] = $value
    }
}

if ($catalogConfig.remote_detected -eq $true -and [string]::IsNullOrWhiteSpace([string]$catalogConfig.config_note)) {
    $catalogConfig.config_note = "Clone URLs and source links use git remote origin at generation time."
}

$configJson = ($catalogConfig | ConvertTo-Json -Compress -Depth 6)

if (-not (Test-Path $catalogDataRoot)) {
    New-Item -ItemType Directory -Path $catalogDataRoot | Out-Null
}

$provider = [string]$catalogConfig.remote_provider
$webBase = [string]$catalogConfig.repo_web_base
$sourceRef = [string]$catalogConfig.source_ref

$skills = @()

Get-ChildItem -Path $skillsRoot -Directory | ForEach-Object {
    $metaPath = Join-Path $_.FullName "meta.json"
    $skillPath = Join-Path $_.FullName "SKILL.md"

    if (-not (Test-Path $metaPath)) {
        Write-Warning "Skipping $($_.Name) because meta.json is missing."
        return
    }

    $meta = Get-Content -LiteralPath $metaPath -Raw -Encoding UTF8 | ConvertFrom-Json

    $dirName = $_.Name
    $skillRel = ("skills/{0}/SKILL.md" -f $dirName) -replace '\\', '/'
    $metaRel = ("skills/{0}/meta.json" -f $dirName) -replace '\\', '/'

    $skillWeb = Resolve-SourceBrowserUrl -Provider $provider -WebBase $webBase -Ref $sourceRef -RelPath $skillRel
    $metaWeb = Resolve-SourceBrowserUrl -Provider $provider -WebBase $webBase -Ref $sourceRef -RelPath $metaRel

    $skill = [ordered]@{
        name = $meta.name
        title = $meta.title
        description = $meta.description
        tags = @($meta.tags)
        owner = $meta.owner
        status = $meta.status
        version = $meta.version
        summary = $meta.summary
        use_cases = @($meta.use_cases)
        install_hint = $meta.install_hint
        skill_path = $skillRel
        meta_path = $metaRel
        skill_web_url = $skillWeb
        meta_web_url = $metaWeb
    }

    $skills += [pscustomobject]$skill
}

$skills = $skills | Sort-Object name
$json = $skills | ConvertTo-Json -Depth 10

Set-Content -Path $jsonOut -Value $json -Encoding UTF8
$jsBundle = "window.CATALOG_CONFIG = " + $configJson + ";" + [Environment]::NewLine + "window.SKILLS_DATA = " + $json + ";" + [Environment]::NewLine
Set-Content -Path $jsOut -Value $jsBundle -Encoding UTF8

Write-Host "Generated catalog data:"
Write-Host " - $jsonOut"
Write-Host " - $jsOut"
