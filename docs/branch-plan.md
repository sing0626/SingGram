# Parallel Branch Plan

Start from the Flutter baseline commit. The old `codex/android-*` branches are historical; new work should use `codex/flutter-*`.

## Branches

| Branch | Main Scope | Goal |
| --- | --- | --- |
| `codex/flutter-liquid-glass-ui` | `lib/src/screens`, `lib/src/widgets` | Polish Liquid Glass chat UI, animations, responsive phone/tablet states, and visual quality settings. |
| `codex/flutter-tdlib-bridge` | `android/`, `lib/src/platform` | Add MethodChannel or Pigeon bridge, TDLib native artifacts, and Android Kotlin engine boundary. |
| `codex/flutter-auth-flow` | `lib/src/data`, `lib/src/models`, `android/` bridge calls | Replace fake login with TDLib authorization states, code login, optional 2FA, logout, reconnect, and flood-wait surfaces. |
| `codex/flutter-message-sync` | `lib/src/data`, `lib/src/models`, `android/` bridge calls | Load chats/messages, subscribe to updates, send text, pagination, and delivery status. |
| `codex/flutter-local-security` | `android/`, `lib/src/platform`, docs | App-private TDLib paths, Android Keystore plan, logout cleanup, crash/log privacy, APK signing safety. |

## Merge Order

1. `codex/flutter-tdlib-bridge`
2. `codex/flutter-auth-flow`
3. `codex/flutter-message-sync`
4. `codex/flutter-liquid-glass-ui`
5. `codex/flutter-local-security`

## Rules

- Do not commit API ID, API hash, login codes, sessions, TDLib database files, APKs, AABs, keystores, or personal logs.
- Keep Dart repository contracts small and reviewed before merging because multiple branches depend on them.
- Prefer explicit user actions for message sending, read behavior, account changes, and privacy-sensitive operations.
- Run `flutter analyze` and `flutter test` before merging.
