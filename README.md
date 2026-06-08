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

## Telegram Setup

1. Create an app at [my.telegram.org](https://my.telegram.org).
2. Keep `api_id` and `api_hash` private.
3. Run the Android app and enter `api_id`, `api_hash`, and your phone number.
4. Enter the Telegram login code, then the 2FA password if your account uses one.
5. After login, the sidebar shows your real Telegram name from TDLib `getMe`.

Do not commit API credentials, login codes, session databases, keystores, APKs, AABs, or TDLib runtime data.

## Branch Plan

See [docs/branch-plan.md](docs/branch-plan.md).
