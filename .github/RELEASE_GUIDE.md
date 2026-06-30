# Release Guide

## One-Click Release

1. Go to GitHub → **Actions** → **Release Pipeline**
2. Click **Run workflow**
3. Choose version type: `patch` / `minor` / `major`
4. Keep `Create Release` checked
5. Click **Run workflow**

CI will:
- Bump version in `build.gradle.kts`
- Increment `versionCode`
- Create git tag `vX.X.X`
- Build all 5 APK architectures + AAB
- Create GitHub Release with assets
- Push upstream PR branch (if applicable)

## Manual Release (alternative)

```bash
# Bump version locally
# Edit nova-radar-android-app/app/build.gradle.kts
# Change appVersionName and versionCode

git add -A
git commit -m "chore: bump version to X.X.X"
git tag -a vX.X.X -m "vX.X.X"
git push origin vX.X.X
```

## First-time Setup

Run once to store your keystore as a GitHub secret:
```powershell
pwsh scripts/setup-secrets.ps1
```
