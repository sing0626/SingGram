# SingGram

SingGram is one repository with two official Telegram clients.

- Android: `official-android/`
- iOS: `official-ios/`

`telegram-ios` is a branch of this repository, not a separate iOS repo. The Android tree stays beside the iOS tree because this branch merges back into `android-official-fork`.

## What This Fork Is

Android:

- Base: official Telegram Android source from `https://github.com/DrKLO/Telegram`
- Current imported upstream: Telegram Android `13.1.1` / version code `7237`
- Package name: `com.sing.singgram`
- Target build: arm64 Android APK
- License: GPL-2.0-or-later, following upstream Telegram Android

iOS:

- Base: official Telegram iOS source from `https://github.com/TelegramMessenger/Telegram-iOS`
- Current imported upstream: Telegram iOS `13.1.2` at `279f89e4d4f63f17227bf877dc3d8990f22ecd58`
- Bundle id: `com.sing.singgram`. Re-sign the Actions IPA with your own certificate and leave this id.
- Same `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` as the Android app
- License: GPL-2.0-or-later, following upstream Telegram iOS

SingGram keeps each official client's architecture and patches that client's own UI, instead of rebuilding Telegram in a separate stack.

## SingGram Changes

- SingGram branding on the Android and iOS clients
- Native Telegram UI on each platform, patched in place
- Liquid Glass on Android's blur pipeline, and iOS SwiftUI `.glassEffect(.regular.interactive(), in: Capsule(style: .continuous))`
- Traditional Chinese and Cantonese-oriented settings polish
- SingGram settings on both clients: ghost mode, kept messages, AI actions, diagnostics, and crash-safe mode
- Android and iOS GitHub Actions builds
- Upstream sync helper for pulling newer Telegram Android releases

## Repository Layout

```text
official-android/                         Official Telegram Android fork
official-ios/                             Official Telegram iOS fork
scripts/build_official_android.sh         Local arm64 release build
scripts/sync_official_android_upstream.sh Sync helper for Telegram upstream
scripts/fetch_official_ios_submodules.sh  Fetch pinned Telegram iOS submodules
scripts/write_singgram_ios_configuration.sh
.github/workflows/official-android-build.yml
.github/workflows/official-ios-build.yml
docs/official-android-fork.md             Fork, build, update, and release notes
```

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

GitHub Actions checks the official Telegram Android upstream on a schedule. If
`DrKLO/Telegram` has moved beyond the imported commit or version recorded in
`.github/singgram-upstream.json`, the checker commits the new upstream metadata,
app version name, README, and fork documentation to `android-official-fork`, then dispatches the public
SingGram release build.

The automatic checker does not blindly merge upstream source code. Use the sync
script below when you want to review and merge Telegram Android source changes
into SingGram.

Use the sync script to merge newer Telegram Android upstream changes into a temporary branch:

```bash
scripts/sync_official_android_upstream.sh master
```

Then resolve conflicts, build, test on a phone, and merge the sync branch only after the APK is usable.

After a reviewed upstream source merge, make sure `.github/singgram-upstream.json`
still points at the imported Telegram commit/version so the scheduled checker
knows the fork is current.

Recommended checks after every upstream update:

1. Login and account switching
2. Chat open/back navigation
3. Message receive and notification behavior
4. Media download and playback
5. SingGram settings and diagnostics
6. Liquid Glass surfaces

More details are in `docs/official-android-fork.md`.

## iOS

`official-ios/` is Telegram iOS `13.1.2` imported from `TelegramMessenger/Telegram-iOS` at `279f89e4d4f63f17227bf877dc3d8990f22ecd58`. The home-screen name in the Bazel app plist is SingGram. Submodule checkouts are not stored in this repository; fetch the pinned commits before building:

```bash
scripts/fetch_official_ios_submodules.sh
TELEGRAM_API_ID=123456 TELEGRAM_API_HASH=your_hash SINGGRAM_IOS_TEAM_ID=YOURTEAM \
  scripts/write_singgram_ios_configuration.sh
```

The configuration file stays untracked. It reuses the same `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` as the Android build.

GitHub Actions workflow `Build SingGram iOS` runs on the `xcode-27` runner. It signs with the upstream fake certificates so the IPA can be downloaded and re-signed locally. The app and its extensions already use bundle id `com.sing.singgram`; the fake team is `C67CF9S4VU`. Leave the bundle id when re-signing. APNs still needs a paid Apple Developer membership and an APNs certificate uploaded for this `api_id` at https://my.telegram.org.

## Security Notes

Do not commit:

- `telegram_credentials.env`
- `google-services.json`
- signing keys or keystores
- APK/AAB build outputs
- session databases or device logs

The public repository should contain source only. Credentials and Firebase config are restored in CI from GitHub Secrets.

## License

Telegram Android and Telegram iOS are GPL-2.0-or-later. SingGram keeps that license for both trees in this repository. If public APKs or IPAs are distributed, publish the matching SingGram source for that release and keep private credentials and signing keys out of the repository.
