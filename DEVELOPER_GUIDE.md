# Nova Radar — Developer & Build Guide

This document provides the complete technical reference for building, signing, and deploying Nova Radar. Any developer (human or AI) working on this project should follow this guide.

---

## 1. System Requirements

| Requirement | Version |
|-------------|---------|
| Android Studio | Koala / Ladybug / Meerkat or newer |
| JDK | **JDK 17** (set in Gradle settings) |
| Kotlin | 2.0.x (via Gradle) |
| Compile SDK | 36 |
| Target SDK | 35 |
| Min SDK | 24 (Android 7.0) |

---

## 2. Project Architecture

```
NovaRadar/
├── app/
│   ├── build.gradle.kts           # Module build config
│   └── src/main/java/com/novaradar/app/
│       ├── MainActivity.kt        # Entry point, nav bar, pager
│       ├── data/
│       │   ├── dao/               # Room DAOs
│       │   ├── database/          # Room database
│       │   ├── model/             # Data classes (IpSource, PortConfig, ScanHistory)
│       │   └── repository/        # NovaRadarRepository
│       └── ui/
│           ├── screens/           # RadarScreen, EasyInstallerScreen, SettingsScreen, LogsScreen, AboutScreen
│           ├── theme/             # NovaRadarTheme, Color, Type
│           ├── localization/      # Localization (EN/FA)
│           └── viewmodel/         # NovaRadarViewModel (scan engine + state)
├── gradle/libs.versions.toml     # Version catalog
├── build.gradle.kts               # Project-level
├── settings.gradle.kts
├── nova-radar-key.jks             # Release keystore (checked in)
└── upload-key.pem                  # PEM for Play Console
```

---

## 3. Import in Android Studio

1. **File → Open** → select the folder containing `settings.gradle.kts`
2. Wait for Gradle sync
3. Set **JDK 17**: `File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK`

---

## 4. Build Commands

### Debug APK
```powershell
.\gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/`

### Release APK (signed)
```powershell
.\gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/NovaRadar-v{version}-{abi}-release.apk`

### Android App Bundle (for Play Store)
```powershell
.\gradlew bundleRelease
```
Output: `app/build/outputs/bundle/release/NovaRadar-v{version}-release.aab`

### Clean build
```powershell
.\gradlew clean assembleRelease
```

---

## 5. Signing Configuration

**Keystore**: `nova-radar-key.jks` (project root)
- **Alias**: `nova-radar`
- **Password**: `NovaRadar2026`
- **V1 signing**: disabled (`enableV1Signing = false`)
- **V2 signing**: enabled (`enableV2Signing = true`)

The signing config is in `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("${rootDir}/nova-radar-key.jks")
        storePassword = "NovaRadar2026"
        keyAlias = "nova-radar"
        keyPassword = "NovaRadar2026"
        enableV1Signing = false
        enableV2Signing = true
    }
}
```

**Upload key PEM**: `upload-key.pem` — extract for Google Play Console App Signing registration.

---

## 6. Versioning

Update in `app/build.gradle.kts`:

```kotlin
val appVersionName = "1.5.0"    // Semantic version
versionCode = 3                  // Increment each release
```

The About screen reads `BuildConfig.VERSION_NAME` automatically.

---

## 7. Screens

The app uses a 5-tab HorizontalPager:

| Index | Tab | File |
|-------|-----|------|
| 0 | EasyInstaller | `Screens.kt` — EasyInstallerScreen |
| 1 | Settings | `Screens.kt` — SettingsScreen |
| 2 | **Radar** | `Screens.kt` — RadarScreen (main scanning UI) |
| 3 | Logs | `Screens.kt` — LogsScreen |
| 4 | About | `Screens.kt` — AboutScreen |

RadarScreen has two sub-tabs (SCANNER / RESULTS) via inner HorizontalPager.

---

## 8. Scanning Engine

Two-phase design:

### Phase 1 — Quick TCP Connect
- 100 concurrent threads, 1500ms timeout
- Raw `Socket().connect()` on all targets
- Records latency in `_recentProbes`

### Phase 2 — Deep Verification
- 50 concurrent threads, 3 attempts per candidate
- TLS ports (443, 2053, 2083, 2087, 2096, 8443): `SSLContext.getDefault()` → `SSLSocket.startHandshake()`
- Non-TLS: TCP + `InputStream.read()`
- Pass if >= 2/3 succeed
- Verified IPs stored in `_allAliveIps` with ping, angle, distance

**Critical rules** (never violate these):
- No custom `X509TrustManager`
- No custom `SNIHostName`
- No reflection for `Socket` / `InetSocketAddress`
- No R8 minification (`isMinifyEnabled = false`)
- Standard Java APIs only

---

## 9. Play Protect

The app may trigger Play Protect on sideloaded devices because:
1. It's a network scanning tool (sockets, IP probes)
2. Not downloaded from Play Store

**Permanent fix**: Upload AAB to Google Play Console (Internal Testing) with Play App Signing enabled. Once Google recognizes the signing certificate, Play Protect stops warning even for sideloaded builds.

---

## 10. Database

- Room database with 3 tables: `IpSource`, `PortConfig`, `ScanHistory`
- Database version: currently **4** (increment for schema changes)
- Default IP sources: only first 2 CIDRs enabled
- Default ports: only 80 and 443 enabled

---

## 11. UI Conventions

- **Radar**: `weight(1f)` + `fillMaxWidth()` — fills available space
- **Stat boxes**: `String.format("%5d", count)` — fixed-width values
- **Bottom nav**: `RoundedCornerShape(32.dp)` border, center item is start/stop button
- **Spacing**: `Arrangement.spacedBy(8.dp)` between scanner elements
- **Theme**: Deep navy (dark) / Clean white (light)
- **Typography**: Vazirmatn (Persian + English)

---

*Last updated: 2026-06-27*
