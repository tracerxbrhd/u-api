param(
    [ValidateSet("auto", "release", "beta", "alpha")]
    [string]$Channel = "auto",

    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FailureMessage Exit code: $LASTEXITCODE"
    }
}

function Get-CheckedOutput {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    $output = & $FilePath @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        $text = ($output | Out-String).Trim()
        if ($text.Length -gt 0) {
            throw "$FailureMessage`n$text"
        }
        throw $FailureMessage
    }
    return ($output | Out-String).Trim()
}

function Test-GitCommand {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)
    & git @Arguments *> $null
    return $LASTEXITCODE -eq 0
}

function Read-GradleProperties {
    param([Parameter(Mandatory = $true)][string]$Path)

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) { continue }
        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) { continue }
        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        $properties[$key] = $value
    }
    return $properties
}

function Require-Property {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Properties,
        [Parameter(Mandatory = $true)][string]$Name
    )

    if (-not $Properties.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Properties[$Name])) {
        throw "gradle.properties does not define required property '$Name'."
    }
    return [string]$Properties[$Name]
}

function Optional-Property {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Properties,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Fallback
    )

    if (-not $Properties.ContainsKey($Name) -or [string]::IsNullOrWhiteSpace($Properties[$Name])) {
        return $Fallback
    }
    return [string]$Properties[$Name]
}

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "Git is not available in PATH."
}

$repoRoot = Get-CheckedOutput git @("rev-parse", "--show-toplevel") "This command must be run inside a Git repository."
$repoRoot = [System.IO.Path]::GetFullPath($repoRoot)
$currentDirectory = [System.IO.Path]::GetFullPath((Get-Location).Path)
if ($currentDirectory.TrimEnd("\") -ne $repoRoot.TrimEnd("\")) {
    throw "Run this script from the repository root: $repoRoot"
}

$gradlePropertiesPath = Join-Path $repoRoot "gradle.properties"
if (-not (Test-Path -LiteralPath $gradlePropertiesPath -PathType Leaf)) {
    throw "gradle.properties was not found in the repository root."
}

$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle Wrapper for Windows was not found: gradlew.bat"
}

$branch = Get-CheckedOutput git @("branch", "--show-current") "Unable to determine the current branch."
if ([string]::IsNullOrWhiteSpace($branch)) {
    throw "Current HEAD is detached. Check out a branch before releasing."
}

$status = Get-CheckedOutput git @("status", "--porcelain=v1", "--untracked-files=all") "Unable to inspect working tree state."
if (-not [string]::IsNullOrWhiteSpace($status)) {
    throw "Working tree is not clean. Commit, stash, or remove all changes and untracked files before releasing.`n$status"
}

$properties = Read-GradleProperties $gradlePropertiesPath
$modVersion = Require-Property $properties "mod_version"
$minecraftVersion = Require-Property $properties "minecraft_version"
$modName = Require-Property $properties "mod_name"
$releaseRemote = Optional-Property $properties "release_remote" "github"

if ($releaseRemote -notmatch "^[A-Za-z0-9._-]+$") {
    throw "release_remote '$releaseRemote' is not a valid Git remote name."
}

Invoke-Checked git @("fetch", $releaseRemote, $branch, "--tags", "--prune") "git fetch failed."

$remoteBranch = "$releaseRemote/$branch"
if (-not (Test-GitCommand @("rev-parse", "--verify", $remoteBranch))) {
    throw "Current branch '$branch' does not exist on remote '$releaseRemote'."
}

$head = Get-CheckedOutput git @("rev-parse", "HEAD") "Unable to read local HEAD."
$remoteHead = Get-CheckedOutput git @("rev-parse", $remoteBranch) "Unable to read $remoteBranch."
if ($head -ne $remoteHead) {
    throw "Local HEAD does not match $remoteBranch. Push or pull before releasing.`nLocal:  $head`nRemote: $remoteHead"
}

$aheadBehind = Get-CheckedOutput git @("rev-list", "--left-right", "--count", "$branch...$remoteBranch") "Unable to compare local branch with $releaseRemote."
$parts = $aheadBehind -split "\s+"
if ($parts.Count -lt 2 -or $parts[0] -ne "0" -or $parts[1] -ne "0") {
    throw "Branch '$branch' is not synchronized with $remoteBranch. Ahead: $($parts[0]); behind: $($parts[1])."
}

if ($modVersion -notmatch "^\d+\.\d+\.\d+(?:-[0-9A-Za-z][0-9A-Za-z.-]*)?$") {
    throw "mod_version '$modVersion' is not supported. Expected SemVer-like value, for example 2.0.0 or 2.1.0-beta."
}
if ($minecraftVersion -notmatch "^\d+\.\d+(?:\.\d+)?$") {
    throw "minecraft_version '$minecraftVersion' is not supported. Expected value like 1.21.1."
}

$inferredChannel = if ($modVersion -match "-alpha(?:[.-]|$)") {
    "alpha"
} elseif ($modVersion -match "-(?:beta|rc)(?:[.-]|$)") {
    "beta"
} else {
    "release"
}
if ($Channel -ne "auto" -and $Channel -ne $inferredChannel) {
    throw "Requested channel '$Channel' does not match mod_version '$modVersion' (inferred '$inferredChannel')."
}
$Channel = $inferredChannel
$publishVersion = "$modVersion+mc$minecraftVersion"
$tagName = "v$publishVersion"

if (Test-GitCommand @("show-ref", "--tags", "--verify", "--quiet", "refs/tags/$tagName")) {
    throw "Tag '$tagName' already exists locally."
}

$remoteTag = Get-CheckedOutput git @("ls-remote", "--tags", $releaseRemote, "refs/tags/$tagName") "Unable to check remote tags."
if (-not [string]::IsNullOrWhiteSpace($remoteTag)) {
    throw "Tag '$tagName' already exists on remote '$releaseRemote'."
}

Write-Host ""
Write-Host "Project:           $modName"
Write-Host "Remote:            $releaseRemote"
Write-Host "Branch:            $branch"
Write-Host "Mod version:       $modVersion"
Write-Host "Minecraft version: $minecraftVersion"
Write-Host "Channel:           $Channel"
Write-Host "Tag:               $tagName"
Write-Host "Commit:            $head"
Write-Host ""

Write-Host "Running clean local build..."
Invoke-Checked $gradleWrapper @("clean", "build") "Gradle build failed. No tag was created."

if ($DryRun) {
    Write-Host ""
    Write-Host "Dry-run completed successfully. Tag '$tagName' was not created or pushed."
    exit 0
}

$tagCreated = $false
try {
    Invoke-Checked git @("tag", "-a", $tagName, "-m", "$modName $publishVersion for Minecraft $minecraftVersion") "Failed to create local tag."
    $tagCreated = $true
    Invoke-Checked git @("push", $releaseRemote, $tagName) "Failed to push tag '$tagName' to remote '$releaseRemote'."
    Write-Host ""
    Write-Host "Release tag pushed: $tagName"
    Write-Host "GitHub Actions will build and publish the release."
} catch {
    if ($tagCreated) {
        Write-Warning "Tag push failed. Removing local tag '$tagName'."
        & git tag -d $tagName *> $null
    }
    throw
}
