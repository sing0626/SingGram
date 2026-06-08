# TDLib Module

This module is the boundary for real Telegram access.

The baseline compiles without TDLib native files. The `codex/android-build-tdlib-native` branch should add one of these:

- Prebuilt TDLib Android artifacts committed through a private artifact repository.
- A local build process that produces `libtdjni.so` for supported ABIs.
- Generated Java bindings under `org.drinkless.tdlib` from the same TDLib version as the native library.

Real authorization belongs in `codex/android-tdlib-auth`; message loading and update handling belongs in `codex/android-message-sync`.
