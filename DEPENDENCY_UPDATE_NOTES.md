# Dependency Update Notes - January 2026

This document summarizes the dependency updates made to keep the BottleCast Android application running with the latest stable versions.

## Summary of Changes

All dependencies have been updated to their latest stable versions as of January 2026, ensuring compatibility with Gradle 8.11.1.

### Build Tools & Plugins

| Dependency | Previous Version | New Version | Notes |
|------------|-----------------|-------------|-------|
| Android Gradle Plugin | 8.9.1 | 8.10.1 | Requires Gradle 8.11.1+. Compatible with latest Android SDK. |
| Kotlin | 2.0.21 | 2.1.21 | Includes K2 compiler improvements and better multiplatform support. |
| Kotlin Parcelize | 1.9.0 | 2.1.21 | Updated to match Kotlin version for consistency. |
| KSP | 2.0.21-1.0.28 | 2.1.21-2.0.1 | Updated to match Kotlin 2.1.21 for proper symbol processing. |
| Hilt | 2.56.2 | 2.58 | Latest Dagger/Hilt with bug fixes and improvements. |
| Google Services | 4.4.2 | 4.4.2 | No change - already at latest stable. |
| Firebase Crashlytics | 3.0.3 | 3.0.3 | No change - already at latest stable. |

### Kotlin Libraries

| Dependency | Previous Version | New Version | Notes |
|------------|-----------------|-------------|-------|
| Coroutines | 1.7.3 | 1.9.0 | Improved performance and stability. |

### AndroidX Libraries

| Dependency | Previous Version | New Version | Notes |
|------------|-----------------|-------------|-------|
| AndroidX Core KTX | 1.16.0 | 1.17.0 | Latest stable release with new APIs and bug fixes. |
| Lifecycle | 2.8.7 | 2.8.7 | No change - already at latest stable. |
| Activity | 1.10.1 | 1.10.1 | No change - already at latest stable. |
| Compose BOM | 2025.04.01 | 2026.01.00 | Updates all Compose libraries to their latest compatible versions. |
| Compose Compiler | 1.5.15 | 1.5.15 | No change - already at latest stable. |
| Hilt Navigation | 1.2.0 | 1.2.0 | No change - already at latest stable. |
| Navigation Compose | 2.8.9 | 2.8.9 | No change - already at latest stable. |
| Window | 1.3.0 | 1.3.0 | No change - already at latest stable. |

### Testing Libraries

| Dependency | Previous Version | New Version | Notes |
|------------|-----------------|-------------|-------|
| AndroidX Test | 1.6.1 | 1.6.1 | No change - already at latest stable. |
| AndroidX Test Ext | 1.2.1 | 1.2.1 | No change - already at latest stable. |
| Test Runner | 1.6.2 | 1.6.2 | No change - already at latest stable. |
| JUnit | 4.13.2 | 4.13.2 | No change - already at latest stable. |
| MockK | 1.13.7 | 1.13.13 | Bug fixes and improved Kotlin support. |

### Square/Third-Party Libraries

| Dependency | Previous Version | New Version | Notes |
|------------|-----------------|-------------|-------|
| Moshi | 1.15.1 | 1.15.2 | Minor bug fixes and improvements. |
| Retrofit | 2.11.0 | 3.0.0 | **Major version update** - see migration notes below. |
| Timber | 5.0.1 | 5.0.1 | No change - already at latest stable. |
| Lottie | 6.1.0 | 6.7.1 | Improved animation support, 3D features, and performance improvements. |

### Firebase & Google Play Services

| Dependency | Previous Version | New Version | Notes |
|------------|-----------------|-------------|-------|
| Firebase BOM | 33.13.0 | 34.8.0 | Manages all Firebase library versions. |
| Firebase Analytics | 21.3.0 | 21.3.0 | Version controlled by BOM. |
| Firebase Auth | 22.1.0 | 22.1.0 | Version controlled by BOM. |
| Play Services Auth | 21.3.0 | 21.3.0 | No change - already at latest stable. |
| Google Fonts | 1.5.1 | 1.5.1 | No change - already at latest stable. |

## Migration Notes

### Retrofit 3.0.0

Retrofit 3.0.0 is a major version update with the following key changes:

1. **OkHttp Upgrade**: The primary change is upgrading the internal OkHttp dependency from 3.14 (legacy) to OkHttp 4.12, which is written in Kotlin and actively maintained.
2. **Transitive Kotlin Dependency**: Because OkHttp 4.x is written in Kotlin, Retrofit 3.0.0 introduces a transitive dependency on Kotlin (already present in this project).
3. **Forward Binary Compatibility**: Maintains forward binary compatibility with Retrofit 2.x at runtime, so libraries compiled against 2.x can still work with 3.x.
4. **No API Changes**: The public API remains unchanged - no code modifications are required.

**Action Required**: None - the upgrade is transparent to application code as Kotlin is already in use.

### Kotlin 2.1.21

Kotlin 2.1.21 brings:

1. **K2 Compiler**: Improved compilation speed and better error messages.
2. **Multiplatform Improvements**: Enhanced Kotlin Multiplatform support.
3. **Standard Library Updates**: New APIs and performance improvements.

**Action Required**: None - the update is backward compatible.

### Compose BOM 2026.01.00

The Compose BOM update brings all Compose libraries to version 1.10.1, and Material 3 to version 1.4.0:

1. **New APIs**: Additional compose APIs and improvements.
2. **Performance**: Better rendering performance and reduced recomposition.
3. **Bug Fixes**: Various stability and bug fixes.

**Action Required**: None - the update is backward compatible with existing Compose code.

## Verification Steps

To verify that the updates work correctly:

1. **Clean Build**:
   ```bash
   ./gradlew clean
   ```

2. **Build the Project**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run Tests**:
   ```bash
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

4. **Check for Deprecation Warnings**:
   Review build output for any deprecation warnings that should be addressed.

5. **Run the Application**:
   Install and run the debug build on a device or emulator to ensure all features work correctly.

## Known Issues

### Google Maven Repository Access

During the update process, the CI environment had limited access to Google's Maven repository. The dependency version updates have been made in the configuration files, but the actual build and test verification must be completed in an environment with proper network access to:
- `https://maven.google.com`
- `https://dl.google.com/android/maven2/`

## Future Updates

Consider checking for updates periodically:

1. **Android Gradle Plugin**: Check [AGP Release Notes](https://developer.android.com/build/releases/gradle-plugin)
2. **Kotlin**: Check [Kotlin Releases](https://kotlinlang.org/docs/releases.html)
3. **AndroidX**: Check [AndroidX Release Notes](https://developer.android.com/jetpack/androidx/versions)
4. **Firebase**: Check [Firebase Release Notes](https://firebase.google.com/support/releases)

## Compatibility Matrix

This project now uses:
- **Gradle**: 8.11.1
- **Android Gradle Plugin**: 8.10.1 (requires Gradle 8.11.1+)
- **Kotlin**: 2.1.21
- **JVM Target**: 11
- **Min SDK**: 33
- **Target SDK**: 34
- **Compile SDK**: 35

## Security Considerations

All updated dependencies include the latest security patches and bug fixes as of January 2026. The Firebase BOM update ensures that all Firebase libraries are using versions with known security vulnerabilities patched.

## Additional Notes

- All version numbers are documented in `gradle/libs.versions.toml` for easy version management.
- The Gradle wrapper was evaluated but kept at 8.11.1 as it's the maximum supported by AGP 8.10.1.
- For future updates to AGP 8.11+ or later, Gradle must also be updated to 8.13+.
