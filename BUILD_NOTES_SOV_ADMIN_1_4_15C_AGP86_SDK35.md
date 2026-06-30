# SOV Admin 1.4.15c — AGP 8.6 / SDK 35 build tooling

This build keeps the same app logic from v1.4.15b and only cleans the Android build tooling for SDK 35.

Changes:
- compileSdk remains 35.
- targetSdk remains 35.
- Android Gradle Plugin upgraded from 8.5.2 to 8.6.1.
- Gradle wrapper distribution changed from 9.0.0 to 8.7.
- Java/Kotlin target remains 17.
- versionCode bumped to 900071.
- versionName: 1.4.15c-armory-web-v615-agp86-sdk35.

Reason:
AGP 8.6 officially supports API 35. Gradle 8.7 + JDK 17 are the compatible baseline for AGP 8.6.
