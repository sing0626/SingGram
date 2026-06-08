# TG Third By Sing

Android-first private Telegram client with a Flutter Liquid Glass UI.

## Direction

The app is Flutter for the Liquid Glass UI and TDLib for the Telegram account session.

- Flutter drives the app shell and visual system.
- `liquid_glass_widgets` provides Liquid Glass-style surfaces and controls.
- `tdlib` provides the Android bundled `libtdjson.so` runtime.
- Android native Kotlin remains available under `android/` for deeper TDLib and MethodChannel work.
- The normal app path now uses real TDLib authorization, not fake login.

This is still Android-first. macOS can come later because Flutter can add a macOS target, but the repo currently only generates the Android platform.

## MVP Target

- API ID/API hash/phone/code login using TDLib authorization states.
- Telegram 2-step verification password when TDLib asks for it.
- Real profile display from `getMe` after login.
- Glass chat list and responsive message view.
- Text composer wired to TDLib `sendMessage`.
- Platform channel boundary for TDLib integration.
- Branches split so UI, auth, sync, native bridge, and local security can move in parallel.

## Run

```bash
flutter pub get
flutter run -d android
```

For a debug APK:

```bash
flutter build apk --debug
```

To build with your Telegram API credentials embedded into the APK:

```bash
cp telegram_credentials.env.example telegram_credentials.env
# edit telegram_credentials.env on your own machine
scripts/build_android_with_credentials.sh
```

When `TELEGRAM_API_ID` and `TELEGRAM_API_HASH` are embedded, the login screen only asks for the phone number. Without embedded credentials, the app asks for API ID/hash once and stores them in Android-backed secure storage for later runs.

## Telegram Setup

1. Create an app at [my.telegram.org](https://my.telegram.org).
2. Keep `api_id` and `api_hash` private.
3. Put `api_id` and `api_hash` in `telegram_credentials.env` if you want them built into your private APK.
4. Enter the Telegram login code, then the 2FA password if your account uses one.
5. After login, the sidebar shows your real Telegram name from TDLib `getMe`.

Do not commit API credentials, login codes, session databases, keystores, APKs, AABs, or TDLib runtime data. `telegram_credentials.env` is ignored by git.

## Branch Plan

See [docs/branch-plan.md](docs/branch-plan.md).
