# SHOP TIME MASTER — Android Native (Kotlin + Jetpack Compose)

Personal Business Time & Productivity Tracker built for shop owners in Bangladesh (`bn-BD` default, `Asia/Dhaka` timezone, `BDT ৳` currency, Bengali numerals `০১২৩৪৫৬৭৮৯` by default).

## Architecture & Tech Stack
- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose + Material 3 (Dark charcoal `#090D0B` + Emerald `#10B981` + Noto Sans Bengali)
- **Architecture**: MVVM + Clean Architecture
- **Database**: Room (`AppDatabase` with 11 normalized entities, foreign keys, indexes, transactions, and `MIGRATION_1_2`)
- **Preferences**: Jetpack DataStore Preferences
- **DI**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`)
- **Location & Background**: Google Play Services Location + Geofencing API + WorkManager + Boot Recovery
- **SDK Targets**:
  - `compileSdk = 35`, `targetSdk = 35`
  - `minSdk = 26` (Android 8.0 Oreo): Chosen to guarantee native `java.time.ZoneId("Asia/Dhaka")`, Notification Channels, `AdaptiveIconDrawable`, and modern `WorkManager` / `BiometricPrompt` compatibility while covering >95% of active Android devices.

## Environment Note
This source directory was generated inside an AI Studio web applet workspace (where JDK/Android SDK are not pre-installed). Open this `ShopTimeMaster` folder in **Android Studio Ladybug (2024.2+)** or newer to sync Gradle and build APKs locally.

## Build Debug APK
```bash
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## Build Signed Release APK
1. Generate a keystore:
```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias shoptimemaster
```
2. Add signing properties in `local.properties` or `gradle.properties` (never commit secrets):
```properties
STM_STORE_FILE=../release-key.jks
STM_STORE_PASSWORD=your_store_password
STM_KEY_ALIAS=shoptimemaster
STM_KEY_PASSWORD=your_key_password
```
3. Run:
```bash
./gradlew assembleRelease
```
Output APK: `app/build/outputs/apk/release/app-release.apk`

## Offline-First & Privacy Guarantees
- All core time tracking, outside reason classification, goals, income per-hour math, leakage detection, daily journal, PDF/CSV export, and rule-based coaching work **100% offline**.
- Location coordinates stay strictly in the local Room database and are never uploaded to external servers or Gemini.
- Optional Gemini AI Coach calls go only through a secure backend proxy with aggregated numeric metrics (no raw GPS).
