# Nova Radar Development Roadmap

This document records the development history, architecture decisions, and build procedures for Nova Radar. Read this first before making any changes to the project.

---

## 1. Project Identity

- **Package**: `com.novaradar.app`
- **Min SDK**: 24 / **Target**: 35 / **Compile**: 36
- **Architecture**: MVVM + Repository + Room
- **UI**: Jetpack Compose + Material 3
- **Language**: Kotlin only

---

## 2. Version History

| Version | Date | Key Changes |
|---------|------|-------------|
| 1.5.0   | 2026-06-27 | Redesigned scanner UI, floating nav button, live probe feed, spacing fix |
| 1.0.02  | 2026-06-25 | Quick/deep scan engine, 2-stage probe, Play Protect fixes |
| 1.0.01  | 2026-06-24 | Initial Play Store-compatible build, Room DB, icon rebrand |

---

## 3. Build & Signing

### Version
Update in `app/build.gradle.kts` line 4:
```kotlin
val appVersionName = "1.5.0"   // change for releases
```
And `versionCode` on line 22 (increment by 1 each release).

### Build Commands
```powershell
# APK (arm64-v8a + universal + others)
.\gradlew assembleRelease

# Android App Bundle (for Play Console upload)
.\gradlew bundleRelease
```

### Signing Config
- **Keystore**: `nova-radar-key.jks` (project root)
- **Alias**: `nova-radar`, Password: `NovaRadar2026`
- **V1 signing**: disabled (`enableV1Signing = false`) — deprecated by Google
- **V2 signing**: enabled — required for Android 7+
- **Upload key PEM**: `upload-key.pem` (for Play Console App Signing registration)
- **SHA256**: `E1:6C:7D:BF:6F:FE:A1:73:8C:50:00:95:6D:CC:9F:EA:2E:ED:A3:51:71:E1:A5:15:C5:1F:69:74:85:09:C3:46`

### Output Files
```
app/build/outputs/apk/release/NovaRadar-v{version}-{abi}-release.apk
app/build/outputs/bundle/release/NovaRadar-v{version}-release.aab
```

---

## 4. Scanning Engine Architecture

### Two-Phase Probe

**Phase 1 — Quick TCP Connect** (`deepTestConnect` equiv `testSocketConnection`)
- Concurrency: 100 threads
- Timeout: 1500ms
- Tests raw TCP `Socket().connect()` on target `ip:port`
- Records success/failure + latency in `_recentProbes`

**Phase 2 — Deep Protocol Verify** (`deepTestConnect`)
- Concurrency: 50 threads
- 3 attempts per candidate, pass if >= 2 succeed
- TLS ports (443, 2053, 2083, 2087, 2096, 8443): uses `SSLContext.getDefault()` → `SSLSocket` → `startHandshake()`
- Non-TLS ports: raw TCP + `InputStream.read()`
- Verified IPs stored as `AliveIp` objects with ping, angle, distance

### Key Design Rules
- **No custom `X509TrustManager`** — triggers Google Play Protect
- **No custom `SNIHostName`** on random IPs — triggers Google Play Protect
- **No reflection** for `Socket`/`InetSocketAddress` — suspicious to Play Protect
- **Standard Java APIs only**: `java.net.Socket`, `java.net.InetSocketAddress`, `javax.net.ssl.SSLContext`

---

## 5. UI Layout (Radar Screen)

The radar screen uses a `HorizontalPager` with two tabs: **SCANNER** and **RESULTS**.

### Scanner Tab layout (top→bottom):
1. **Radar canvas** — `weight(1f)`, fills all space, animated sweep with dots for top 8 clean IPs
2. **HUD line** — subnet status + ETA
3. **4 Stat boxes** (2×2 grid) — SCANNED, ALIVE, DEAD, ETA (fixed-width `String.format`)
4. **PROBE FEED** — 60dp scrolling log of live probe results
5. **CLEAN FOUND** — top 3 verified IPs with ping

### Results Tab:
- MFD-style black/green terminal with `allAliveIps` (sorted by ping)
- Toolbar: Copy Top 10, Copy All, Save to TXT

### Bottom Navigation Bar:
- 5 items with glassmorphism style
- Center item (radar) is the **start/stop button** — red gradient, `PowerSettingsNew` icon, pulse animation when scanning
- `RoundedCornerShape(32.dp)` border (no notch)
- Selected item: circular glassy indicator

---

## 6. Play Protect History (Critical Context)

**Problem**: App flagged by Google Play Protect on sideloaded devices.

**Root causes found & fixed**:
1. **Custom `X509TrustManager`** — trust-all SSL. Removed entirely.
2. **Custom `SNIHostName`** — sending `nova2.altramax083.workers.dev` to random IPs during TLS probes. Removed; now uses `SSLContext.getDefault()` without SNI override.
3. **Reflection for Socket/InetSocketAddress** — suspicious to Play Protect. Removed; now uses direct Java APIs.

**What remains**: Play Protect still warns because the app is not from Play Store. This is expected behavior for ANY sideloaded network tool. The only permanent fix: upload to Google Play Console (even Internal Testing), which registers the signing cert with Google.

> **2026-06-27 note**: Version 1.5.0 AAB is ready for Play Console upload. Once uploaded with Play App Signing, Play Protect will stop flagging even sideloaded copies.

---

## 7. Git Workflow

- **Branch**: `main` for releases
- **Tags**: Annotated tags (`git tag -a vX.X.X -m "..."`)
- **Commit style**: short descriptive messages in English

---

## 8. Rules for AI Agents

- **Never add custom TrustManagers or SNI overrides** — triggers Play Protect
- **Never use reflection for Socket/InetSocketAddress**
- **Never enable R8 minification** — breaks the scanner engine
- **Never change scanner threading model** without testing
- **Keep V1 signing disabled** (`enableV1Signing = false`)
- **Keep V2 signing enabled** (`enableV2Signing = true`)
- **All socket timeouts must be explicit** — no infinite waits
- **Radar canvas uses `weight(1f)`** — never fixed height
- **Stat values use `String.format("%5d", ...)`** — prevent box width changes
- **Spacing between scanner elements**: `Arrangement.spacedBy(8.dp)`

---

*Last updated: 2026-06-27*
