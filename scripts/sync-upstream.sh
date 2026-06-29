#!/bin/bash
# Upstream sync script for CI (Linux/bash)
# Reads .upstream/config.json, filters files, creates upstream-base branch
set -euo pipefail

CONFIG="${1:-.upstream/config.json}"

echo "=== Upstream Sync (CI) ==="

# Parse config with jq or fallback to python
if command -v jq &>/dev/null; then
    SOURCE_BRANCH=$(jq -r '.fork.sourceBranch' "$CONFIG")
    TARGET_BRANCH=$(jq -r '.fork.upstreamBranch' "$CONFIG")
    INCLUDE_FILE=$(jq -r '.filter.includeFile' "$CONFIG")
    EXCLUDE_FILE=$(jq -r '.filter.excludeFile' "$CONFIG")
else
    SOURCE_BRANCH=$(python3 -c "import json; print(json.load(open('$CONFIG'))['fork']['sourceBranch'])")
    TARGET_BRANCH=$(python3 -c "import json; print(json.load(open('$CONFIG'))['fork']['upstreamBranch'])")
    INCLUDE_FILE=$(python3 -c "import json; print(json.load(open('$CONFIG'))['filter']['includeFile'])")
    EXCLUDE_FILE=$(python3 -c "import json; print(json.load(open('$CONFIG'))['filter']['excludeFile'])")
fi

echo "Source: $SOURCE_BRANCH → Target: $TARGET_BRANCH"

# Read include patterns (skip blank lines and comments)
mapfile -t INCLUDE_PATTERNS < <(grep -v '^\s*#' "$INCLUDE_FILE" | grep -v '^\s*$')

# Read exclude patterns
EXCLUDE_PATTERNS=()
if [ -f "$EXCLUDE_FILE" ]; then
    mapfile -t EXCLUDE_PATTERNS < <(grep -v '^\s*#' "$EXCLUDE_FILE" | grep -v '^\s*$')
fi

# Clean working tree and switch to source branch
git reset --hard HEAD 2>/dev/null || true
git clean -fd 2>/dev/null || true
git checkout -f "$SOURCE_BRANCH"

echo "Filtering tracked files..."
ALL_FILES=$(git ls-files)

# Filter: include if matches any include pattern AND doesn't match any exclude
FILTERED=()
while IFS= read -r f; do
    MATCH=0
    for pat in "${INCLUDE_PATTERNS[@]}"; do
        # shellcheck disable=SC2053
        if [[ "$f" == $pat ]]; then
            MATCH=1
            break
        fi
    done
    if [ "$MATCH" -eq 1 ]; then
        for pat in "${EXCLUDE_PATTERNS[@]}"; do
            # shellcheck disable=SC2053
            if [[ "$f" == $pat ]]; then
                MATCH=0
                break
            fi
        done
    fi
    if [ "$MATCH" -eq 1 ]; then
        FILTERED+=("$f")
    fi
done <<< "$ALL_FILES"

echo "Included: ${#FILTERED[@]} / $(echo "$ALL_FILES" | wc -l) files"

if [ "${#FILTERED[@]}" -eq 0 ]; then
    echo "ERROR: No files matched include patterns"
    exit 1
fi

# Save list for debugging
mkdir -p .upstream
printf '%s\n' "${FILTERED[@]}" > .upstream/last-sync-files.txt

# Create orphan branch (clean slate)
git checkout --orphan "$TARGET_BRANCH"
git rm -rf --cached . 2>/dev/null || true

# Remove all tracked files from worktree
for f in $(git ls-files); do
    rm -f "$f" 2>/dev/null || true
done

echo "Copying files from $SOURCE_BRANCH..."
COUNT=0
for f in "${FILTERED[@]}"; do
    dir=$(dirname "$f")
    if [ "$dir" != "." ] && [ ! -d "$dir" ]; then
        mkdir -p "$dir"
    fi
    if git checkout "$SOURCE_BRANCH" -- "$f" 2>/dev/null; then
        COUNT=$((COUNT+1))
    else
        echo "WARNING: Failed to copy $f" >&2
    fi
done

echo "Copied $COUNT files"

git add -A

# Extract version for commit message
VERSION="sync"
if [ -f "nova-radar-android-app/app/build.gradle.kts" ]; then
    VERSION=$(grep -oP 'appVersionName\s*=\s*"\K[^"]+' "nova-radar-android-app/app/build.gradle.kts" || echo "sync")
fi

git commit -m "upstream: sync Android app v${VERSION} from ${SOURCE_BRANCH}"

echo "=== Sync Complete ==="
echo "Branch '$TARGET_BRANCH' ready for upstream PR (v${VERSION})"
