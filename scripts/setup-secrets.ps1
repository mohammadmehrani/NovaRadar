# Script to upload keystore as GitHub secret
# Run: pwsh scripts/setup-secrets.ps1
param(
    [string]$Repo = "mohammadmehrani/NovaRadar",
    [string]$KeystorePath = "nova-radar-android-app/nova-radar-key.jks"
)

if (-not (Test-Path $KeystorePath)) {
    Write-Host "Keystore not found at $KeystorePath. Generating..." -ForegroundColor Yellow
    & "$env:JAVA_HOME/bin/keytool" -genkey -v `
        -keystore $KeystorePath `
        -alias "nova-radar" `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -storepass "123456" -keypass "123456" `
        -dname "CN=NovaRadar, OU=Development, O=NovaProxy, L=Tehran, ST=Tehran, C=IR"
}

# Base64 encode and create secret
$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $KeystorePath))
$b64 = [Convert]::ToBase64String($bytes)
$env:KEYSTORE_B64 = $b64

gh secret set KEYSTORE_B64 --repo $Repo --body $b64
gh secret set KEYSTORE_PASSWORD --repo $Repo --body "123456"
gh secret set KEY_ALIAS --repo $Repo --body "nova-radar"
gh secret set KEY_PASSWORD --repo $Repo --body "123456"

Write-Host "Secrets uploaded to $Repo" -ForegroundColor Green
