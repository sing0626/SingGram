# SingGram notification investigation

SIMExpiryManager was inspected at `e4d3b799fdc74cb2c7b56614d992ef7be5fd0791`.
Its `services/notifications.ts` schedules local reminders, and its
`app/index.tsx` schedules a local test after five seconds. Its default iOS
prebuild deliberately removes `aps-environment` (`plugins/withIosWidget.js`).
Those notifications establish local permission/display behavior, not Telegram
server-to-APNs delivery. No signing private key is needed to compare this code.

## Implemented checks

Settings → SingGram → Diagnostics offers notification status, native permission
and registration, and a five-second local test. These actions are available
without enabling the general diagnostics toggle. The local test is explicitly
labelled as local and has a distinct identifier handled by the foreground
notification delegate. Permission denial completes the registration callback
instead of leaving callers waiting.

Apple registration callbacks and Telegram `account.registerDevice` responses are
observed independently. Telegram observations are isolated by local account
record and reset when the process exits. A copied report contains neither tokens,
notification keys/payloads nor server error descriptions. Existing Telegram
registration retries/return behavior are preserved. A server accepting a token
does not establish that a remote notification was delivered.

The report labels provisioning-profile metadata separately from live iOS
registration state. Profile metadata describes permitted capabilities, not the
final executable entitlements. App Group availability is checked with the real
container API. No account database is moved into a different shared container.

## Required device checks

Before recording, expect three native rows on the Diagnostics page. Each opens
without clipping in light/dark appearance and with larger text. Opening and
closing the status alert should preserve scroll position; no full token or
account identifier should appear in the alert or copied report.

1. On a fresh install, native registration requests permission. If denied, the
   action finishes with an explanation and a link to iPhone Settings. After
   allowing notifications there, repeat registration and inspect the new status.
2. Run the local test with the app in front, on the home screen, and locked.
   Expect one notification approximately five seconds later. Repeating before
   firing replaces the pending test rather than stacking copies. A local test
   must not be counted as a Telegram remote-delivery test.
3. Check Apple registration, Telegram registration for the selected account,
   the profile environment/identity match, and expected App Group availability.
   Switch accounts and verify one account's success is not attributed to another.
4. With the phone locked, send a real message from another Telegram account.
   Note whether the alert arrives, whether content is processed by the extension,
   and whether the message only appears after opening the app. Compare this with
   the registration observations. Backend APNs topic/certificate configuration
   remains unverified by a local success or accepted registration response.

These interactions require the user's signed device build. Linux source checks
and the macOS build do not establish real push delivery or visual correctness.
Follow `official-ios/AGENTS.md` to evaluate timestamped device/simulator frames.
Keep recordings, signing profiles and user account data out of public commits.
