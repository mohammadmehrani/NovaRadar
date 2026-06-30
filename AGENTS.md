# Nova Radar — Full Project Reference

Read this first to understand the project architecture, build process, scanning engine, UI conventions, and critical constraints.

---

## 1. Project Identity

| Field | Value |
|-------|-------|
| Package | `com.novaproxy.scanner` (changed from com.novaradar.app for Play Protect) |
| Version | `1.3.0` (semantic, in `app/build.gradle.kts`) |
| Version Code | `2` (increment each release) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 |
| Compile SDK | 36 |
| Architecture | MVVM + Repository + Room |
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin only |
| Repository | `https://github.com/mohammadmehrani/NovaRadar` |
| Upstream | `https://github.com/IRNova/NovaRadar` |
| App icon | Top-left on pages without icons (no top header) |

---

## 2. Project Structure

```
NovaRadar/
├── nova-radar-android-app/          # Android project root
│   ├── app/
│   │   ├── build.gradle.kts         # Module build config, version, signing
│   │   └── src/main/java/com/novaradar/app/
│   │       ├── MainActivity.kt      # Entry point, sticky bottom nav, pager
│   │   ├── proxy/
│   │   │   ├── TunnelService.kt   # SOCKS5 proxy tunnel → TunnelStats
│   │   │   ├── LinkParser.kt      # Parse VLESS/VMESS/Trojan/SS links → ProxyNode
│   │   │   ├── NodeStore.kt       # ProxyNode list + RemoteFeed subscriptions
│   │   │   ├── RouteGuard.kt      # GeoIP Iran + geosite + ad-block routing
│   │   │   └── GeoTable.kt        # GeoIP lookup database (RouteDecision, GeoRange)
│   │   ├── data/
│   │   │   ├── dao/             # Room DAOs
│   │   │   ├── database/        # Room database (version 4, fallbackToDestructiveMigration)
│   │   │   ├── model/           # IpSource, PortConfig, ScanHistory
│   │   │   └── repository/      # NovaRadarRepository
│   │   └── ui/
│   │       ├── screens/         # RadarScreen, SettingsScreen, ImportScreen, AboutScreen, ClientScreen
│   │       ├── components/      # WidgetCard, ParticleBackground (glass card + animated bg)
│   │       ├── theme/           # Theme, Color, Type (Vazirmatn font)
│   │       ├── localization/    # Localization (EN/FA)
│   │       └── viewmodel/       # NovaRadarViewModel (scan engine + tunnel + all state)
│   ├── build.gradle.kts             # Project-level
│   ├── settings.gradle.kts
│   ├── gradle/libs.versions.toml    # Version catalog
│   ├── gradlew / gradlew.bat
│   └── build.ps1                    # PowerShell build helper
├── nova-radar-key.jks               # Release keystore (gitignored by *.jks pattern)
└── scanner core.html                # Reference HTML: original scanner features (speed test, copy dropdown, advanced settings, ping filter)
```

---

## 3. Version History

| Version | Date | Key Changes |
|---------|------|-------------|
| 1.0.0 | 2026-06-27 | Production release: final radar UI, wizard fix, GitHub Actions |
| 1.0.02 | 2026-06-25 | Quick/deep scan engine, 2-stage probe, Play Protect fixes |
| 1.0.01 | 2026-06-24 | Initial Play Store-compatible build, Room DB, icon rebrand |
| 1.1.0 | 2026-06-28 | Tag/release #1 to main, CI fixed, JDK 21 + keystore password 123456 |
| 1.2.0 | 2026-06-29 | Green ATC-style circular radar, sticky bottom nav, compact list-style ResultsTab, all HTML features ported |
| 1.3.0 | 2026-06-29 | Play Protect fix (appId change), ping filter slider, copy dropdown per row, speed test visual feedback |

---

## 4. Build & Signing

### Requirements
- Android Studio Koala+ / JDK 21 (Gradle 9.6)
- JDK path: `C:\Program Files\Android\Android Studio\jbr`
- ANDROID_HOME: `$env:LOCALAPPDATA\Android\Sdk`

### Build Commands
```powershell
# Set environment
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd nova-radar-android-app

# Release APKs (all 5 architectures)
.\gradlew assembleRelease --no-daemon

# Clean build
.\gradlew clean assembleRelease --no-daemon

# Build with rerun (force recompile)
.\gradlew assembleRelease --no-daemon --rerun-tasks
```

### Signing Config (app/build.gradle.kts)
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("${rootDir}/nova-radar-key.jks")
        storePassword = "123456"
        keyAlias = "nova-radar"
        keyPassword = "123456"
        enableV1Signing = true
        enableV2Signing = true
    }
}
```

### Keystore Info
- **File**: `nova-radar-key.jks` (keystore may be at root or android project level)
- **Alias**: `nova-radar`
- **Password**: `123456`
- **Algorithm**: RSA 2048
- **Fingerprint**: `96:F6:B9:B1:49:D1:FE:6D:01:C2:82:67:D9:2A:48:89:12:0C:7A:7A:27:4A:DC:AC:4D:80:EC:2E:68:5C:55:F0`
- **Note**: CI uses password `"123456"`, keystore must be present in the repo root for CI to sign

### Output Files
```
app/build/outputs/apk/release/NovaRadar-v{version}-{abi}-release.apk
  ABIs: arm64-v8a, armeabi-v7a, universal, x86, x86_64
```

### R8 / ProGuard
- `isMinifyEnabled = false` — **MUST stay disabled** (breaks scanner engine)
- No R8 minification, no ProGuard rules apply

---

## 5. Scanning Engine

### Two-Phase Probe Design (matches Go original IRNova/NovaRadar logic)

**Phase 1 — Quick TCP Connect**
- 64 concurrent threads (changed from 100)
- Timeout: 1500ms
- Raw `Socket().connect()` on all `ip:port` targets
- Records latency in `_recentProbes`

**Phase 2 — Deep Verification**
- 50 concurrent threads
- 3 attempts per candidate, pass if ≥ 2 succeed
- TLS ports (443, 2053, 2083, 2087, 2096, 8443): `SSLContext.getDefault()` → `SSLSocket.startHandshake()`
- Custom SNI via `SSLParameters.setServerNames()` with `vlessSNI = "nova2.altramax083.workers.dev"` (matches Go `tls.Config{ServerName: ...}`)
- Non-TLS ports: TCP + `InputStream.read()`
- Verified IPs stored as `AliveIp` objects (ping, angle, distance, port, isTls)
- HTTP ping is also performed as part of verification

### Critical Rules (NEVER violate these)
- **No custom `X509TrustManager`** — triggers Play Protect "Harmful app"
- `SSLParameters.setServerNames()` IS acceptable (standard Java API)
- **No reflection** for `Socket` / `InetSocketAddress` — suspicious to Play Protect
- **Standard Java APIs only**: `java.net.Socket`, `java.net.InetSocketAddress`, `javax.net.ssl.SSLContext`
- **R8 minification MUST stay disabled**
- **All socket timeouts must be explicit** — no infinite waits
- **Radar canvas uses `weight(1f)`** — never fixed height
- **Stat values use `String.format("%5d", ...)`** — prevent box width changes
- **Spacing between scanner elements**: `Arrangement.spacedBy(8.dp)`

### IP Sources
Three groups (stored in Room database: `IpSource` table):
- **Cloudflare** (45 CIDR ranges) — default enabled
- **Akamai** (6 CIDR ranges)
- **Vercel** (14 CIDR ranges)
- Only Cloudflare enabled by default
- Multiple CIDRs per source separated by `,`
- `generateIpsForSubnet()` does fair sampling across all CIDRs
- CIDR display removed from SettingsScreen

### Default Ports
- Ports 80 and 443 enabled by default
- TLS ports: 443, 2053, 2083, 2087, 2096, 8443
- All ports shown in SettingsScreen (inside WidgetCard)

---

## 6. Screens & Navigation

### Tab Structure (HorizontalPager with 4 tabs)

| Index | Tab | Screen File | Description |
|-------|-----|-------------|-------------|
| 0 | Settings | `SettingsScreen.kt` | Client config + IP sources + ports + speed limit |
| 1 | **Radar** | `RadarScreen.kt` | Main scanning UI (Scanner + Results + Client sub-tabs) |
| 2 | Import | `ImportScreen.kt` | Import IP:port list |
| 3 | About | `AboutScreen.kt` | App info, credits, terminal log |

### Bottom Navigation (MainActivity.kt)
- **Sticky** at bottom (NOT floating)
- `Column` layout: content `weight(1f)` + `NavigationBar` at bottom
- `NavigationBar` with `fillMaxWidth()`, `height(64.dp)`, `RoundedCornerShape(topStart=20dp, topEnd=20dp)`
- Center item is the start/stop radar button
- No individual page padding needed (managed by parent)

### RadarScreen Sub-tabs (inner HorizontalPager)
- **Scanner Tab**: Green ATC-style radar (square card, circular), HUD line, 4 stat boxes, probe feed, clean found list
- **Results Tab**: Compact list-style table with all HTML features
- **Client Tab**: VPN client dashboard (connect/disconnect, traffic, profile management)

---

## 7. Green ATC Radar Design (RadarScreen.kt)

### Visual Elements
- `radarGreen = Color(0xFF00FF66)` — green radar theme
- Square `WidgetCard` with `CircleShape` clip + dark green background
- 4 concentric range rings (semi-transparent green)
- Crosshairs: Horizontal + Vertical + Diagonal lines
- Sweep gradient brush (transparent → green) + sweep line
- Center dot with pulsing ring
- 10 dots positioned by IP:port hash (stable across recompositions)
- Dot colors: green (<200ms), amber (200-500ms), red (≥500ms)
- Sweep highlight + fade mechanism when sweep passes over a dot
- Ping labels on top-5 dots
- `FullscreenRadarDialog` uses same green ATC radar + TOP 10 list

### Data Display
- HUD line: subnet status + ETA
- 4 stat boxes (2×2 grid): SCANNED, ALIVE, DEAD, ETA (fixed-width `String.format("%5d")`)
- PROBE FEED: 60dp scrolling log of live probe results
- CLEAN FOUND: top verified IPs with ping

---

## 8. Results Tab (Compact List-Style)

### Features (porting all HTML scanner functionality)
- Table header: `# / IP / PING / HTTP / SPD / ACT`
- Each row: compact with port and TLS tags
- ⚡ Speed test button per row (calls `viewModel.runSpeedTest()`)
- Copy per row (dropdown: "Copy IP" / "Copy IP:Port")
- HTTP ping status
- Latency bar below each row (visual ping indicator)
- SPD column shows speed test result
- Toolbar: Copy Top 10, Copy All, Save to TXT, Copy All IP:Port

### Remaining HTML features to port
- Ping filter slider in tools section
- Ensure ⚡ speed test gives visual feedback (disable button briefly, show "⏳" then result)

---

## 9. Play Protect — Critical Context

### Problem
App flagged as "Harmful app" by Google Play Protect on sideloaded devices.

### Root Causes Found & Fixed
1. **Custom `X509TrustManager`** — trust-all SSL. Removed entirely.
2. **Custom `SNIHostName`** on random IPs during TLS probes. Fixed: now uses `SSLParameters.setServerNames()` (standard Java API, acceptable).
3. **Reflection** for Socket/InetSocketAddress. Removed.

### Current Status
- `SSLParameters.setServerNames()` is acceptable (NOT flagged)
- New keystore (RSA 2048, fresh fingerprint) helps but may not fully break association
- Package name `com.novaradar.app` may still be tied to old flagged certificate in Play Protect database
- **"Unknown app"** warning is acceptable; "Harmful app" is NOT

### Mitigation Strategies (in order of effectiveness)
1. **Change applicationId** to something new like `com.novaproxy.scanner` (breaks package-name association)
2. **Upload AAB to Google Play Console** (Internal Testing) with Play App Signing — once Google recognizes the signing certificate, Play Protect stops warning even for sideloaded builds
3. Submit app for Play Protect review from the Play Console
4. App is ONLY on GitHub (no Play Store, no Cafe Bazaar/Myket)

---

## 10. Theme & Design

### Colors
- **Dark theme**: Deep navy backgrounds, green accents for radar
- **Light theme**: Clean white, explicit contrast colors in WidgetCard (`lightText`, `lightTextSecondary`, `lightBg`, `lightCard`, `lightBorder`)
- Material color scheme-based (dark/light auto-switch)

### Typography
- **Vazirmatn** font for Persian + English text
- Three weights: Regular, Medium, Bold

### WidgetCard Component (`WidgetCard.kt`)
- `Wc` color palette object with explicit light/dark colors
- `WidgetCard` composable: glass card with rounded corners, used for radar, stat boxes, settings sections
- Light theme content must be clearly visible with sufficient color contrast

---

## 11. Database

- **Room** database with 3 tables: `IpSource`, `PortConfig`, `ScanHistory`
- **Database version**: currently `4` (increment for schema changes)
- `fallbackToDestructiveMigration()` — new installs get clean DB, old users lose data on upgrade
- Default IP sources: only first 2 CIDRs enabled
- Default ports: only 80 and 443 enabled

---

## 12. CI/CD (GitHub Actions)

### Workflow: `android.yml`
- Trigger: tags matching `v*` (e.g., `v1.2.0`)
- JDK 21 (NOT 17 — 17 broke with Gradle 9.x)
- Builds all 5 APK architectures + AAB
- Signs with keystore password `"123456"`
- Creates GitHub Release with all APKs + AAB as assets
- Keystore path in CI: `${{ github.workspace }}/nova-radar-key.jks`

### Release Process
1. Commit changes to branch
2. Create PR to main
3. Push tag `vX.X.X` to trigger CI
4. CI builds, signs, creates release with APKs + AAB

---

## 13. Git Workflow

- **Main branch**: `main` for releases
- **Feature branches**: named like `v1.2.0-redesign`
- **Tags**: Annotated (`git tag -a vX.X.X -m "..."`)
- **PRs**: Created from feature branch to main
- **Commit style**: Short descriptive messages in English

---

## 14. Rules for AI Agents

1. **Never add custom `X509TrustManager`** — triggers Play Protect
2. **Never use reflection** for Socket/InetSocketAddress
3. **Never enable R8 minification** (`isMinifyEnabled = false`)
4. **Never change scanner threading model** without testing
5. **Keep V1 + V2 signing enabled** (both true)
6. **All socket timeouts must be explicit**
7. **Radar canvas uses `weight(1f)`** — never fixed height
8. **Stat values use `String.format("%5d", ...)`** — prevent box width changes
9. **Spacing between scanner elements**: `Arrangement.spacedBy(8.dp)`
10. **Don't add comments to code** unless asked
11. **Don't add emojis** unless asked
12. **Don't create README or doc files** unless asked
13. **Bottom nav is sticky** at bottom, not floating
14. **Radar is green ATC-style** inside square card
15. **Results Tab is compact list-style** with all HTML features

---

## 15. Key Dependencies (libs.versions.toml)

- Jetpack Compose BOM
- Material 3
- Room (KSP)
- Navigation Compose
- Horologist (Compose tools)

---

## 16. Key Technical Details

- `vlessSNI = "nova2.altramax083.workers.dev"` — set via `SSLParameters.setServerNames()`, matches Go original
- Speed test: `runSpeedTest()` function in ViewModel (line ~229), `speedResults` StateFlow
- Speed limit slider: 1–20 MB/s in SettingsScreen (default 10)
- Ping filter slider in ResultsTab header
- Copy dropdown per row: Copy IP / Copy IP:Port
- Copy all dropdown: Copy IPs / Copy IP:Port / Copy Full List
- Rank badges: S(<80ms), A(<200ms), B(<400ms), C(>=400ms)
- Ping comparison chart showing top 15 IPs as horizontal bars
- ParticleBackground: animated floating particles in background
- `coerceAtMost()` used instead of `minOf` (resolves `Unresolved reference` bug with JDK 21 + Gradle 9.x)
- Proxy package renamed: TunnelService, LinkParser, NodeStore, RouteGuard, GeoTable (no Karing references)

---

## 17. Automated Release Pipeline

### One-Click Release
1. Go to GitHub → **Actions** → **Release Pipeline**
2. Click **Run workflow**
3. Choose version type: `patch` / `minor` / `major`
4. CI auto-bumps `appVersionName` + `versionCode`, creates tag, builds APKs, creates release

### Workflow Files
- `.github/workflows/android.yml` — full pipeline (version bump + build + release + upstream PR)
- `.github/workflows/auto-upstream-pr.yml` — upstream PR only (triggered by release publish)

### Keystore (CI)
- Stored as GitHub secret `KEYSTORE_B64` (base64-encoded `.jks`)
- Fallback: auto-generated temporary keystore if secret missing
- Run `pwsh scripts/setup-secrets.ps1` to upload

### Manual Tag (alternative to workflow_dispatch)
```bash
git tag -a vX.X.X -m "vX.X.X"
git push origin vX.X.X
```

---

## 18. Related Projects

### SuperNova
- **Repo**: https://github.com/mohammadmehrani/SuperNova (PRIVATE)
- **Description**: VPN client + Radar IP scanner with VpnService + Xray/sing-box cores
- **Architecture**: VpnService → Core Engine (Xray/v2fly/sing-box) → Radar Scanner
- **Status**: Phase 1 - VpnService Core (in development)
- **Build**: `.\gradlew assembleRelease --no-daemon`

### HyperNova  
- **Repo**: https://github.com/mohammadmehrani/HyperNova (PRIVATE)
- **Description**: SuperNova + Network Tools (WiFi Analyzer, Port Scanner, Traceroute, etc.)
- **Architecture**: Same as SuperNova + Tools layer
- **Status**: Phase 1 - Base Setup (in development)
- **Build**: `.\gradlew assembleRelease --no-daemon`

---

*Last updated: 2026-06-30 | Version: 1.3.0 | Pure Radar Scanner Edition*
