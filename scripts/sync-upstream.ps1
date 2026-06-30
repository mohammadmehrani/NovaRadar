param(
    [string]$ConfigFile = ".upstream\config.json"
)

$ErrorActionPreference = "Stop"

# Load config
$config = Get-Content $ConfigFile | ConvertFrom-Json
$sourceBranch = $config.fork.sourceBranch
$targetBranch = $config.fork.upstreamBranch
$includeFile = $config.filter.includeFile
$excludeFile = $config.filter.excludeFile

Write-Host "=== Upstream Sync ===" -ForegroundColor Cyan
Write-Host "Source: $sourceBranch → Target: $targetBranch"
Write-Host "Include: $includeFile"
Write-Host "Exclude: $excludeFile"

# Load patterns
$includePatterns = Get-Content $includeFile |
    Where-Object { $_ -and ![string]::IsNullOrWhiteSpace($_) -and !$_.StartsWith("#") }

$excludePatterns = @()
if (Test-Path $excludeFile) {
    $excludePatterns = Get-Content $excludeFile |
        Where-Object { $_ -and ![string]::IsNullOrWhiteSpace($_) -and !$_.StartsWith("#") }
}

# Ensure we're on source branch
git checkout $sourceBranch 2>$null
if ($LASTEXITCODE -ne 0) { throw "Failed to checkout $sourceBranch" }

Write-Host "`nFiltering tracked files..." -ForegroundColor Yellow
$allFiles = git ls-files

$filteredFiles = @()
foreach ($f in $allFiles) {
    $include = $false
    foreach ($pat in $includePatterns) {
        if ($f -like $pat) { $include = $true; break }
    }
    if ($include) {
        $exclude = $false
        foreach ($pat in $excludePatterns) {
            if ($f -like $pat) { $exclude = $true; break }
        }
        if (-not $exclude) {
            $filteredFiles += $f
        }
    }
}

Write-Host "Included: $($filteredFiles.Count) / $($allFiles.Count) files"
if ($filteredFiles.Count -eq 0) { throw "No files matched include patterns" }

# Save list to temp file for debugging
$filteredFiles | Out-File ".upstream\last-sync-files.txt"

# Create upstream branch (orphan = clean slate, no history)
git checkout --orphan $targetBranch 2>$null
if ($LASTEXITCODE -ne 0) { throw "Failed to create orphan branch $targetBranch" }

# Remove all files from index
git rm -rf --cached . 2>$null

# Clean worktree: remove everything except .git
Get-ChildItem -Path . -Directory | Where-Object { $_.Name -ne ".git" } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path . -File | Remove-Item -Force -ErrorAction SilentlyContinue

Write-Host "`nCopying files from $sourceBranch..." -ForegroundColor Yellow
$count = 0
$errors = @()
foreach ($f in $filteredFiles) {
    $parent = Split-Path $f -Parent
    if ($parent -and !(Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    & git checkout $sourceBranch -- $f 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        $errors += $f
    } else {
        $count++
    }
}

if ($errors.Count -gt 0) {
    Write-Host "WARNING: $($errors.Count) files failed to copy" -ForegroundColor Yellow
}

Write-Host "Copied $count files successfully" -ForegroundColor Green

git add -A 2>$null
$hasChanges = git diff --cached --name-only
if (-not $hasChanges) {
    Write-Host "WARNING: No changes to commit — upstream branch is empty!" -ForegroundColor Red
    Write-Host "Creating placeholder to avoid empty branch..." -ForegroundColor Yellow
    $placeholder = ".upstream-sync-placeholder"
    "Last sync: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Out-File $placeholder
    git add $placeholder
}

$versionName = "sync"
$versionFile = "nova-radar-android-app/app/build.gradle.kts"
if (Test-Path $versionFile) {
    $content = Get-Content $versionFile -Raw
    if ($content -match 'appVersionName\s*=\s*"([^"]+)"') {
        $versionName = $matches[1]
    }
}

git commit -m "upstream: sync Android app v${versionName} from ${sourceBranch}"

Write-Host "`n=== Sync Complete ===" -ForegroundColor Cyan
Write-Host "Branch '$targetBranch' is ready for upstream PR (v${versionName})"
