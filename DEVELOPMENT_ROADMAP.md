# 🛰️ Nova Radar Development & Release Roadmap

This document serves as the **Standard Operating Procedure (SOP)** for Nova Radar. Any AI Agent or developer working on this project must adhere to these guidelines to ensure consistency, security, and professional release standards.

---

## 🏗️ 1. Project Identity & Architecture
- **Official Package Name**: `com.novaradar.app`
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles.
- **Database**: Room (SQL storage for Scan History, Ports, and IP Sources).

---

## 🛠️ 2. Build & Signing Configuration (Gradle)
The project uses a centralized versioning system in `app/build.gradle.kts`.

### **Versioning Source of Truth**
```kotlin
// app/build.gradle.kts
val appVersionName = "1.0.02" // UPDATE THIS FOR RELEASES
val appVersionCode = 2        // INCREMENT THIS FOR RELEASES
```
The UI (About Screen) dynamically pulls this value via `BuildConfig.VERSION_NAME`.

### **Signing Configuration**
- **Keystore**: `nova-radar-key.jks` (Root of project).
- **Passwords**: Encrypted/Injected via environment variables or hardcoded for internal automation as `NovaRadar2026`.
- **Signing Versions**: V1, V2, V3, and V4 are enabled for maximum Play Protect compatibility.

---

## 🎨 3. Design System Standards
- **Backgrounds**: Must remain transparent/mesh-based to show the global gradient.
- **Typography**: **Vazirmatn** is the mandatory font family for ALL text (Persian and English).
- **Colors**:
    - **Dark**: Pure Black background with `#05111B` / `#0A1A2F` mesh accents.
    - **Light**: Pure White background with `#FFFBEB` / `#FDE68A` mesh accents.
    - **Components**: Vibrant Mesh Gradient `(0xFF22D3EE, 0xFF818CF8, 0xFFA855F7)`.

---

## 🔄 4. Git Branching & Commit Standards
Follow the **GitFlow** model for professional development.

### **Branching Strategy**
- `main`: Production-ready stable code.
- `develop`: Ongoing integration for the next release.
- `feature/xxx`: New features (branched from `develop`).
- `release/vX.X.X`: Stabilization before a new release (branched from `develop`).

### **Commit Message Format**
Use Conventional Commits:
- `feat: ...` for new features.
- `fix: ...` for bug fixes.
- `ui: ...` for design updates.
- `refactor: ...` for code cleanup.
- `chore: ...` for build script updates.

---

## 🚀 5. Automated Release Process (Agent Instructions)
When a release is triggered, the AI Agent must perform these steps in order:

1.  **Sync Version**: Update `appVersionName` and `appVersionCode` in `app/build.gradle.kts`.
2.  **Commit Change**: `git commit -m "chore: bump version to vX.X.X"`.
3.  **Tag Release**: `git tag -a vX.X.X -m "Release vX.X.X"`.
4.  **Local Build**: Run `./gradlew clean app:assembleRelease`.
5.  **GitHub Release (Automation)**:
    ```powershell
    gh release create v1.0.02 "./app/build/outputs/apk/release/NovaRadar-v1.0.02-universal-release.apk" --title "Nova Radar v1.0.02 Official" --notes "Release Notes Here"
    ```
6.  **Verify UI**: Ensure the "About Us" screen displays the matching version.

---

## ⚠️ 6. Important Notes for Future Agents
- **No com.example**: Never use placeholder package names.
- **Play Protect**: If the app shows a warning, verify the signing config in `build.gradle.kts` and ensure the APK is built using the `release` task.
- **Responsiveness**: Use `BoxWithConstraints` or `weight` modifiers. Never hardcode screen heights that cause unwanted scrolling on small devices.

---
*Created on 2026-06-24 for Nova Radar Project.*
