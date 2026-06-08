# Parallel Branch Plan

Start from the Android baseline commit. Keep the branches narrow so people can work at the same time without touching the same files too much.

## Branches

| Branch | Main Scope | Goal |
| --- | --- | --- |
| `codex/android-build-tdlib-native` | `tdlib/build.gradle.kts`, `tdlib/src/main` | Add native TDLib artifacts or build pipeline for `arm64-v8a` and emulator targets. |
| `codex/android-tdlib-auth` | `tdlib/`, `core/src/main/kotlin/.../repository` | Wire TDLib authorization states, API credentials, code login, 2FA, logout, and reconnect. |
| `codex/android-message-sync` | `tdlib/`, `core/src/main/kotlin/.../model` | Load chats/messages, subscribe to updates, send text, and expose pagination. |
| `codex/android-chat-ui` | `app/src/main/java/.../ui` | Polish Compose dialog list, message bubbles, responsive phone/tablet layout, loading, empty, and error states. |
| `codex/android-local-security` | `app/src/main/java/.../data`, `tdlib/`, docs | App-private TDLib directories, Android Keystore plan, logout cleanup, and privacy checklist. |

## Merge Order

1. `codex/android-build-tdlib-native`
2. `codex/android-tdlib-auth`
3. `codex/android-message-sync`
4. `codex/android-chat-ui`
5. `codex/android-local-security`

## Rules

- Do not commit API ID, API hash, login codes, sessions, TDLib database files, APKs, keystores, or personal logs.
- Keep `core` contracts reviewed before merging because multiple branches depend on them.
- Prefer explicit user actions for message sending, read behavior, account changes, and privacy-sensitive operations.
- Run `gradle :app:assembleDebug` or Android Studio's build before merging when the local SDK is available.
