# GameBridge v0.19 — Build Pipeline

Android-native prototype for inspecting game files, creating a port manifest, and exercising a small native demo pipeline.

## Build
The GitHub Actions workflow installs Java 17, Android SDK platform 35/build-tools 35.0.0, Gradle 8.9, and runs `gradle assembleDebug`. Download the APK from the workflow's artifact named `GameBridge-debug-apk` after a successful run.

## Important limitation
This is a porting workbench/prototype, not a universal Windows EXE-to-APK converter. It does not emulate Windows, bypass DRM, or port arbitrary PC games automatically. A real game port requires the relevant source/engine or a title-specific compatible implementation and licensed assets.

This source package has not been compiled or device-tested in this environment. Verify the GitHub Actions build before installing the APK.
