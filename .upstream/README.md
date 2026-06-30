# Upstream PR Management

This directory controls which files are sent to `IRNova/NovaRadar` in upstream PRs.

## How It Works

1. **`main` branch** has ALL files (personal + upstream)
2. **`upstream-base` branch** has only upstream-approved files (auto-generated)
3. On release, CI runs `scripts/sync-upstream.sh` which:
   - Reads `.upstream/include.txt` (what to send)
   - Reads `.upstream/exclude.txt` (what to skip)
   - Creates `upstream-base` branch with ONLY matched files
   - Pushes PR to IRNova/NovaRadar

## Files

| File | Purpose |
|------|---------|
| `config.json` | Connection settings (fork, upstream, branch names) |
| `include.txt` | Glob patterns of files to send upstream |
| `exclude.txt` | Glob patterns of files to EXCLUDE (overrides include) |
| `last-sync-files.txt` | Debug log of last sync |

## Adding New Files to Upstream

Edit `.upstream/include.txt` and add a glob pattern.

## Keeping Files Private

Don't add them to `include.txt`. They stay in `main`, never go to `upstream-base`.
