# SingGram iOS UI refresh: validation

## Implemented surfaces

- Chat list: 17 pt-relative semibold titles, quieter dates, more separation between
  title/sender/preview; compact, balanced and spacious avatar/row metrics. The same
  avatar metrics are used during setup and layout. Existing unread/pin/mute states,
  swipe actions, editing, accessibility labels and message actions remain native.
- Navigation: narrower horizontal floating capsule, more internal padding and
  larger dynamically scaled labels. The parent still owns layout and safe-area /
  keyboard placement. iPad side placement keeps the existing inset calculation.
- Composer: compact mode keeps attachment and AI controls from expanding into a
  second pill. Tap + still opens attachments; long-press opens a native tool menu.
  SingGram AI actions are explicit, disabled for an empty draft, and only offered
  when enabled. Native AI compose remains accessible when available. Inserting a
  result uses `replacingFlatRange`, preserving the editor's structured content.
- Settings: a four-category home (Appearance, Privacy, AI, Diagnostics) replaces
  the long all-in-one list. Existing privacy/AI/safety controls remain available.
- Appearance: palettes, bubble corner styles, list density, system text size,
  composer mode, navigation spacing and glass strength. Colors, bubbles and font
  preferences use the existing native theme storage. Display-only preferences
  use an independent Foundation store with safe defaults and invalid-value
  fallbacks. Glass follows light/dark appearance, Reduce Transparency and Reduce
  Motion. Disabled glass uses an opaque surface rather than a clear backdrop.

## Required visual checks (not yet verified)

No XcodeBuildMCP, simulator, Apple SDK or logged-in iOS UI is available in this
Linux task. The macOS Actions build checks Swift parsing, appearance preference
persistence/fallbacks and the full release target. A successful build is not
visual verification. Record actual interactions before calling the UI finished.

Expected recordings, in light and dark modes and with large accessibility text:

1. Chat list scrolling with unread, pinned, muted, long-title, draft and archived
   rows: title/sender/preview must stay inside each row, dates and unread badges
   must not overlap titles, avatars remain vertically centered. Switch all three
   densities and return to the list; native list-font changes must remeasure it.
2. Switching bottom tabs: icon and label move together, selected state stays
   visible, badges do not clip. On iPad, side tabs stay in their original position.
   Rotating and opening search/keyboard must not cover content or move the capsule
   outside the safe area. Toggle extra navigation spacing and verify it updates.
3. Composer: tap + adds attachments; long-press + opens the tool menu. Test empty
   and multiline drafts, recording/preview, send-as and bot-menu controls, keyboard
   transitions, AI disabled/configuration failure/success, and insertion into a
   selection within a quote or structured block. No automatic AI request is made.
4. Open each of the four settings categories, return, change palette and bubble
   radius, then switch light/dark. Changes persist across relaunch and the native
   appearance controller can still change them. No privacy setting resets.
5. Toggle glass off/standard/strong, Reduce Transparency and Reduce Motion: text
   stays readable; reduced transparency is opaque and reduced motion removes
   style-change animations. Setting safety mode still suppresses glass and AI.

Follow official-ios/AGENTS.md: record with `simctl io recordVideo`, extract labelled
frames using `tools/animation_sheet.py`, and report clipping/jumps/settling with
frame indexes and timestamps. A phone screen recording can additionally show the
signed build on the affected device. Do not substitute a mockup for these checks.
