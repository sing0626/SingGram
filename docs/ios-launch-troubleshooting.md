# iOS launch diagnosis and regression checks

Build from the `telegram-ios` source tree and use the freshly produced IPA. The
workflow's build number now uses the Actions run number plus `build_number_offset`,
so a shallow checkout no longer gives every build the same number. IPA and dSYM
artifacts are saved before release publication to preserve the matching build.

## Changes covered

- Optional executable-signature metadata is parsed using bounded byte reads.
  Missing, ad-hoc, truncated or unsupported signatures yield no metadata instead
  of reading past the mapped file. This does not change iOS signature validation.
- The BoringSSL certificate reader consumes DER CMS data and allocates its output
  stack before parsing. Certificate and key ownership is released after copying
  metadata.
- Push registration requires a parsed `Entitlements.aps-environment` of
  `development` or `production` in the embedded profile; unrelated text is not
  treated as a capability. Both VoIP and ordinary notification registration use
  that decision. A profile is an upper bound on capabilities, not proof of the
  executable's signed entitlements.
- Background URL sessions set a shared-container identifier only when that app
  group is accessible. The existing persistent account-container fallback stays
  intact; this change does not delete or reset account data.

## Checks

From the repository root, on a C compiler with AddressSanitizer/UBSan:

```sh
cc -std=c11 -Wall -Wextra -Werror -fsanitize=address,undefined \
  official-ios/submodules/BuildConfigExtra/Tests/SignatureDataTests.c \
  -o /tmp/signature-data-tests
/tmp/signature-data-tests
```

The test exercises the production parser with valid 32/64-bit and fat Mach-O
fixtures, all truncated prefixes and byte mutations of headers, lengths and
signature contents. It does not exercise Apple's loader or a real re-signed IPA.

On a Swift/Foundation host (also run by the macOS Actions workflow):

```sh
swiftc official-ios/submodules/TelegramUI/Sources/SingGram/SingGramLaunchConfiguration.swift \
  official-ios/submodules/TelegramUI/Tests/LaunchConfiguration/main.swift \
  -o /tmp/launch-configuration-tests
/tmp/launch-configuration-tests
```

The profile tests cover missing, malformed, truncated, unrelated and valid push
entitlements. The full supported iOS build remains the Make.py invocation in
`.github/workflows/official-ios-build.yml`; it needs the macOS/Xcode runner.

## Confirming a device crash

A launch repair is not confirmed until the new IPA is built, signed, installed
and launched on the affected device. Record the iPhone/iOS version, Actions run,
IPA build number and signing tool. Keep the corresponding `Telegram.DSYMs.zip`.

On the iPhone, open Settings → Privacy & Security → Analytics & Improvements →
Analytics Data and export the matching SingGram/Telegram `.ips` report. Its
exception, termination reason and crashed-thread stack distinguish an in-app
crash from a loader/code-signing rejection before app code runs. If iOS rejects
the executable's signature or a bundled framework before `main`, an app-source
fallback cannot repair that rejection: the exact signed IPA and signing setup
must be examined.

Local Linux validation for this change: C parser sanitizer tests, BoringSSL API
syntax check, workflow YAML/inline-shell parsing and the existing 44 Python
build-system tests passed. No local Swift compiler, Apple SDK, simulator or
physical iPhone was available; the Swift checks and full iOS build are pending CI.
