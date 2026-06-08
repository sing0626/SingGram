# TG Third By Sing

Android-first private Telegram client scaffold.

## Language Choice

Use Kotlin for the Android app.

- Kotlin has first-class Android tooling.
- Jetpack Compose is Kotlin-native and keeps the UI fast to iterate.
- Telegram TDLib exposes Java/JNI bindings, which Kotlin can call directly.
- A Kotlin domain layer leaves room for a later macOS client without putting macOS code in this Android repo now.

If the goal changes to "complete Telegram clone as fast as possible", forking Telegram Android is a different path. For a private app we can shape ourselves, this repo uses Kotlin + Compose + TDLib boundaries.

## MVP Target

- Login flow shell for API ID, API hash, phone, code, and optional password.
- Dialog list.
- Message pane.
- Text sending.
- TDLib adapter module separated from UI so auth, sync, and storage can be built in parallel branches.

The current baseline uses a fake in-memory repository so the Android UI can be developed before native TDLib is wired.

## Run

Open the project in Android Studio and run the `app` configuration.

Command-line build requires Gradle and Android SDK installed:

```bash
gradle :app:assembleDebug
```

## Telegram Setup

1. Create an app at [my.telegram.org](https://my.telegram.org).
2. Keep `api_id` and `api_hash` private.
3. Wire TDLib native libraries in the `tdlib` module before real login.

Do not commit API credentials, login codes, session databases, keystores, APKs, or TDLib runtime data.

## Branch Plan

See [docs/branch-plan.md](docs/branch-plan.md).
