# Re-signed iOS launch abort in CloudKit

The supplied device crash report identifies Telegram 13.1.2 build 3732,
iPhone18,3, iOS 27.0 (24A437). It records EXC_CRASH / SIGABRT about 0.24 seconds
after launch. The last Objective-C exception backtrace enters CloudKit (image
31) directly from TelegramCore on a background signal subscription. The report
does not include the exception's reason string or symbolicated TelegramCore
frames, so its exact exception message remains unconfirmed.

The corresponding source path is `Network.swift`'s optional iCloud backup-address
source → `CloudData.fetchRawData` → `CKContainer.default()`. The Actions workflow
forced `SINGGRAM_IOS_ENABLE_ICLOUD=true`, generating iCloud services/container
entitlements for the build's identity. A different signer is used when installing
with ESign. Missing or unusable iCloud entitlements can cause an Objective-C
exception at container initialization; Swift signal error handling cannot catch
that exception.

The re-signable Actions build now sets `SINGGRAM_IOS_ENABLE_ICLOUD=false`.
The configuration writer already defaults to false, and Network.swift checks
`arguments.isICloudEnabled` before creating the CloudData context. Normal
Telegram servers, seed addresses, login and message history do not depend on
this optional CloudKit emergency-address source. Builds with properly configured
iCloud signing can opt in separately through the configuration writer.

New artifacts use Actions run number + build_number_offset instead of shallow
commit count + offset, to distinguish the new IPA from the repeated 3732 builds.
The workflow saves the IPA and matching dSYM archive before publication. The
`fix/ios-cloudkit-launch` branch runs CI but does not publish a public prerelease.
The previous signature-parser hardening branch is separate and is not included
in this minimal crash fix.

Validation: locally exercised the configuration writer with the workflow's
non-secret settings and synthetic test-only API placeholders; generated
`enable_icloud` was false. Explicit opt-in still generated true. Workflow YAML
and inline shell syntax passed. Full IPA compilation and launch on the affected
ESign-signed device remain required; this Linux environment has no Apple SDK.
