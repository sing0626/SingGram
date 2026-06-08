# TG Third By Sing

Android-first private Telegram client with a Flutter Liquid Glass UI.

## Direction

The app is now Flutter for UI and Android native Kotlin for the Telegram engine bridge.

- Flutter drives the app shell and visual system.
- `liquid_glass_widgets` provides Liquid Glass-style surfaces and controls.
- Android native Kotlin remains available under `android/` for TDLib and MethodChannel work.
- The current baseline uses fake in-memory Telegram data so UI work can move before TDLib is wired.

This is still Android-first. macOS can come later because Flutter can add a macOS target, but the repo currently only generates the Android platform.

## MVP Target

- API ID/API hash/phone/code login shell.
- Glass chat list and responsive message view.
- Text composer with fake local sending.
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
3. Wire TDLib native libraries and Java bindings on the Android side.
4. Expose only typed operations to Flutter through MethodChannel or Pigeon.

Do not commit API credentials, login codes, session databases, keystores, APKs, AABs, or TDLib runtime data.

## Branch Plan

See [docs/branch-plan.md](docs/branch-plan.md).
