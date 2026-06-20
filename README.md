# SingGram

SingGram is an Android fork based on the official Telegram Android source. The production app lives in `official-android/`; the older Flutter prototype remains in this repository only as reference material.

## What This Fork Is

- Base: official Telegram Android source from `https://github.com/DrKLO/Telegram`
- Current imported upstream: Telegram Android `12.7.3` / version code `6750`
- Package name: `com.sing.singgram`
- Target build: arm64 Android APK
- License: GPL-2.0-or-later, following upstream Telegram Android

SingGram keeps Telegram's native Android client architecture and patches the official app surfaces directly instead of rebuilding Telegram in a separate UI stack.

## SingGram Changes

- SingGram branding and package configuration
- Native Telegram Android UI as the base client
- Liquid Glass enabled through Telegram's native glass/blur pipeline
- Traditional Chinese and Cantonese-oriented settings polish
- SingGram settings, diagnostics, and push notification checks
- Android build scripts for local and GitHub Actions builds
- Upstream sync helper for pulling newer Telegram Android releases

## Repository Layout

```text
official-android/                         Official Telegram Android fork
scripts/build_official_android.sh         Local arm64 release build
scripts/sync_official_android_upstream.sh Sync helper for Telegram upstream
.github/workflows/official-android-build.yml
docs/official-android-fork.md             Fork, build, update, and release notes
```

The old Flutter prototype folders are still present for history, but new production work should target `official-android/`.

## Local Build

Create a local credentials file first:

```bash
cp telegram_credentials.env.example telegram_credentials.env
```

Then edit `telegram_credentials.env` locally:

```properties
TELEGRAM_API_ID=123456
TELEGRAM_API_HASH=your_hash
SINGGRAM_FORCE_LIQUID_GLASS=true
```

Build:

```bash
scripts/build_official_android.sh
```

Default output:

```text
official-android/TMessagesProj_App/build/outputs/apk/arm64/release/app.apk
```

For a debug APK:

```bash
SINGGRAM_OFFICIAL_TASK=:TMessagesProj_App:assembleArm64Debug scripts/build_official_android.sh
```

## GitHub Actions Build

The workflow can be run manually from GitHub Actions. Required repository secrets:

```text
TELEGRAM_API_ID
TELEGRAM_API_HASH
GOOGLE_SERVICES_JSON_BASE64
```

Create the Firebase secret from a local `google-services.json`:

```bash
base64 -i official-android/TMessagesProj/google-services.json | gh secret set GOOGLE_SERVICES_JSON_BASE64
```

If `publish_release` is enabled when running the workflow, it publishes:

```text
SingGram-arm64-release.apk
update.json
```

Stable public update manifest:

```text
https://github.com/<owner>/<repo>/releases/latest/download/update.json
```

Stable public APK link:

```text
https://github.com/<owner>/<repo>/releases/latest/download/SingGram-arm64-release.apk
```

## Updating From Telegram

Use the sync script to merge newer Telegram Android upstream changes into a temporary branch:

```bash
scripts/sync_official_android_upstream.sh master
```

Then resolve conflicts, build, test on a phone, and merge the sync branch only after the APK is usable.

Recommended checks after every upstream update:

1. Login and account switching
2. Chat open/back navigation
3. Message receive and notification behavior
4. Media download and playback
5. SingGram settings and diagnostics
6. Liquid Glass surfaces

More details are in `docs/official-android-fork.md`.

## Security Notes

Do not commit:

- `telegram_credentials.env`
- `google-services.json`
- signing keys or keystores
- APK/AAB build outputs
- session databases or device logs

The public repository should contain source only. Credentials and Firebase config are restored in CI from GitHub Secrets.

## License

Telegram Android is GPL-2.0-or-later. SingGram keeps that license inheritance for the forked Android source. If public APKs are distributed, publish the matching SingGram source for that release and keep private credentials/signing keys out of the repository.
