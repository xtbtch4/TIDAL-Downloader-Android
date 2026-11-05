# TIDAL Downloader for Android

Android 16 / ARM64 project for managing TIDAL links and preparing an authorized-download workflow.

> This initial commit is an Android application scaffold. It does not yet download or decrypt TIDAL streams. Integration must use authorized APIs and content access; DRM or access-control circumvention is out of scope.

## Build

GitHub Actions builds a debug APK on every push to `main` and via **Actions → Android APK → Run workflow**. Download the artifact named `TIDAL-Downloader-debug` from the completed run.

## Requirements

- Android Studio / JDK 17
- Android SDK 35
- Android 8.0+ (API 26); tested target: Android 16

## Device

The project targets `arm64-v8a` compatible Android devices; the initial UI itself is architecture-independent.
