# SingGram Official Android Fork

This branch pivots from the Flutter prototype to the official Telegram Android source as the base UI and client engine.

## Source Base

- Upstream: `https://github.com/DrKLO/Telegram`
- Imported under: `official-android/`
- Imported upstream commit: `9fea7264 update to 12.7.3 (6750)`
- License: GPL-2.0 or later, as shipped by upstream.

The Flutter prototype remains in the repository for reference only. New production work should target `official-android/`.

## Private Build Credentials

Do not commit API credentials. The official fork reads them at build time from either:

- `telegram_credentials.env`
- exported environment variables
- `official-android/local.properties`

Supported keys:

```properties
TELEGRAM_API_ID=123456
TELEGRAM_API_HASH=your_hash
SINGGRAM_FORCE_LIQUID_GLASS=true
```

Inside the official Android project these become:

- `BuildConfig.SINGGRAM_API_ID`
- `BuildConfig.SINGGRAM_API_HASH`
- `BuildConfig.SINGGRAM_FORCE_LIQUID_GLASS`

`BuildVars.APP_ID` and `BuildVars.APP_HASH` use those values, with upstream dummy values only as fallback.

## Liquid Glass Direction

Telegram Android 12.7.3 already contains a native glass pipeline:

- `org.telegram.ui.Components.blur3.LiquidGlassEffect`
- `org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode`
- `org.telegram.messenger.LiteMode.FLAG_LIQUID_GLASS`

This fork forces `FLAG_LIQUID_GLASS` on Android 13+ when `SINGGRAM_FORCE_LIQUID_GLASS=true`. That keeps the official Android information architecture intact and applies glass through Telegram's own native rendering layer.

## Build

```bash
scripts/build_official_android.sh
```

By default this builds the smaller private release APK for `arm64-v8a` only. Both the app module and native Telegram library module are filtered to `arm64-v8a`.

```text
official-android/TMessagesProj_App/build/outputs/apk/arm64/release/app.apk
```

For a faster targeted Gradle check, override the task:

```bash
SINGGRAM_OFFICIAL_TASK=:TMessagesProj:compileDebugJavaWithJavac scripts/build_official_android.sh
```

For a debuggable APK, override the task:

```bash
SINGGRAM_OFFICIAL_TASK=:TMessagesProj_App:assembleArm64Debug scripts/build_official_android.sh
```

The official project requires Android SDK 35, Build Tools 35.0.0, NDK 27.2.12479018, CMake 3.10.2, and Gradle 8.7.

## Updating From Telegram Upstream

Keep SingGram changes as a normal fork branch, then merge new Telegram Android releases into a temporary sync branch.

The workflow at `.github/workflows/check-telegram-upstream.yml` checks
`https://github.com/DrKLO/Telegram.git` every six hours and can also be started
manually. It compares the upstream ref with `.github/singgram-upstream.json`;
when a newer upstream commit exists, it opens an `upstream-update` issue with the
latest commit. It does not auto-merge upstream into SingGram, because upstream
changes can conflict with branding, settings, Liquid Glass, diagnostics,
notifications, and update logic.

```bash
scripts/sync_official_android_upstream.sh master
```

The script works inside `official-android/` and will:

- add the upstream remote `https://github.com/DrKLO/Telegram.git` if missing
- fetch upstream tags and branches
- create a branch like `singgram/sync-master-YYYYMMDD-HHMM`
- merge the upstream ref without committing, so conflicts can be reviewed

After conflicts are resolved:

```bash
scripts/build_official_android.sh
git -C official-android status
git -C official-android commit
```

Recommended update rhythm:

1. Sync upstream into a temporary branch.
2. Resolve conflicts in SingGram-owned files first: `SingGram*`, branding strings, Gradle package config, Liquid Glass hooks, notification/push diagnostics.
3. Build `:TMessagesProj_App:assembleArm64Release`.
4. Install on a test phone and check login, chat open, media download, notifications, Liquid Glass, Ghost mode, and AI tools.
5. Merge the sync branch back into the working SingGram branch only after the APK is usable.
6. Update `.github/singgram-upstream.json` with the imported upstream
   commit/version.

## GitHub Builds

Use GitHub Actions for repeatable builds. Put private credentials in repository secrets:

- `TELEGRAM_API_ID`
- `TELEGRAM_API_HASH`
- `GOOGLE_SERVICES_JSON_BASE64`

The workflow at `.github/workflows/official-android-build.yml` can be started manually from GitHub Actions and uploads the APK artifact. It builds the same default task as the local script:

```text
:TMessagesProj_App:assembleArm64Release
```

Create the Firebase secret from the local SingGram `google-services.json` without committing that file:

```bash
base64 -i official-android/TMessagesProj/google-services.json | gh secret set GOOGLE_SERVICES_JSON_BASE64
```

The workflow restores that Firebase config into the official Android modules during CI, so FCM keeps working while the public repository stays clean.

### Public Release Channel

The fork should continue to track the official Telegram Android repository:

```text
https://github.com/DrKLO/Telegram.git
```

If the SingGram repository is public, enable `publish_release` when manually running the workflow. The workflow will publish two public GitHub Release assets:

```text
SingGram-arm64-release.apk
update.json
```

The stable public update manifest URL is:

```text
https://github.com/<owner>/<repo>/releases/latest/download/update.json
```

The stable public APK URL is:

```text
https://github.com/<owner>/<repo>/releases/latest/download/SingGram-arm64-release.apk
```

The generated `update.json` contains `versionCode`, `versionName`, `apkUrl`, `sha256`, and `notes`, so SingGram can later check this URL directly for in-app updates without embedding a GitHub token in the APK.

If you distribute APKs publicly, remember that Telegram Android is GPL-2.0-or-later. Public APK releases should have matching SingGram source available for that release, with private signing keys and API secrets excluded.

## Next UI Targets

Keep the official layout and patch shared surfaces only:

- `ActionBar/ActionBar.java` for top bars.
- `ActionBar/BottomSheet.java` and sheet internals for modal chrome.
- `Components/ChatActivityEnterView.java` for the composer.
- `ChatActivity.java`, `DialogsActivity.java`, `TopicsFragment.java`, and `MainTabsActivity.java` where upstream already creates Liquid Glass blur factories.

Do not rebuild Telegram screens in a separate UI framework.
