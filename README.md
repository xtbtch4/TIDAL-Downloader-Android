# TIDAL Downloader for Android

Android 16 / ARM64 project based on the TIDAL ecosystem.

## Current version: 0.2.0

- OAuth 2.1 Authorization Code + PKCE sign-in.
- Deep-link callback handling.
- Official TIDAL Web API client for reading track and album metadata by TIDAL URL.
- No password collection; the user signs in on TIDAL's own login page.

This is an in-progress Android port. It is **not yet a music downloader**. Protected audio manifests, DRM decryption, and bypassing TIDAL access controls are not implemented.

## One-time TIDAL developer setup

1. Create an application in the [TIDAL Developer Portal](https://developer.tidal.com/).
2. Add this exact redirect URI to the application:
   `com.xtbtch4.tidaldownloader://oauth/callback`
3. Copy the application's Client ID.
4. In the Android app, paste the Client ID and tap **Войти через TIDAL**.
5. Complete sign-in on TIDAL's page. Return to the app and inspect a track or album URL.

The app uses TIDAL's documented OAuth authorization-code flow with PKCE. The access token is currently held in memory and is cleared when the app process exits; sign in again after a full restart.

## Build

GitHub Actions builds a debug APK on every push to `main`. Open **Actions → Android APK**, then download the `TIDAL-Downloader-debug` artifact from a successful run.

## Requirements

- JDK 17
- Android SDK 35
- Android 8.0+ (API 26); target device: Android 16 / ARM64
