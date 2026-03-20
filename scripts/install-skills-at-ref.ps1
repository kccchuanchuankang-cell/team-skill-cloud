<#
.SYNOPSIS
  Clone skills-cloud at a Git tag or branch, then run openskills install on that path.

.DESCRIPTION
  OpenSkills "install <git-url>" clones the default branch only. This script clones
  -b <Ref> (tag or branch name) with --depth 1, installs from the local folder, then removes the temp dir.

  For a raw commit SHA, use manual git fetch/checkout (see docs/VERSIONING_AND_OPENSKILLS.md).

.EXAMPLE
  .\scripts\install-skills-at-ref.ps1 -Ref v1.0.0
.EXAMPLE
  .\scripts\install-skills-at-ref.ps1 -Source https://github.com/your-org/skills-cloud.git -Ref v1.0.0 -Universal -Yes
#>
param(
    [string]$Source = "git@github.com:your-org/skills-cloud.git",
    [Parameter(Mandatory = $true)]
    [string]$Ref,
    [switch]$Universal,
    [switch]$Yes
)

$ErrorActionPreference = "Stop"

$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ("skills-cloud-" + [Guid]::NewGuid().ToString("n"))

try {
    Write-Host "Cloning $Source at ref $Ref ..."
    git clone -c advice.detachedHead=false --depth 1 --branch $Ref $Source $tmp
    if ($LASTEXITCODE -ne 0) {
        throw "git clone failed (is Ref a valid tag/branch on this remote? For commit SHA see docs/VERSIONING_AND_OPENSKILLS.md)."
    }

    $args = @("--yes", "openskills", "install", $tmp)
    if ($Universal) { $args += "--universal" }
    if ($Yes) { $args += "-y" }

    Write-Host "npx $($args -join ' ')"
    & npx @args
    if ($LASTEXITCODE -ne 0) {
        throw "npx openskills install failed."
    }

    Write-Host "Done. Run: npx openskills sync"
}
finally {
    if (Test-Path $tmp) {
        Remove-Item -LiteralPath $tmp -Recurse -Force -ErrorAction SilentlyContinue
    }
}
