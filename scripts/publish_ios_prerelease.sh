#!/usr/bin/env bash
set -euo pipefail

IPA="${1:?Usage: publish_ios_prerelease.sh path/to/SingGram-unsigned.ipa}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${IOS_BUILD_SHA:=${GITHUB_SHA:-}}"
: "${IOS_BUILD_NUMBER:=${GITHUB_RUN_NUMBER:-}}"
: "${IOS_BUILD_ATTEMPT:=${GITHUB_RUN_ATTEMPT:-}}"
: "${IOS_BUILD_URL:=https://github.com/$GITHUB_REPOSITORY/actions/runs/${GITHUB_RUN_ID:-}}"

# Validate the local asset and source identity before changing any release.
test -s "$IPA"
[[ "$IOS_BUILD_SHA" =~ ^[0-9a-f]{40}$ ]]
[[ "$IOS_BUILD_NUMBER" =~ ^[0-9]+$ ]]
[[ "$IOS_BUILD_ATTEMPT" =~ ^[0-9]+$ ]]
TAG="ios-${IOS_BUILD_NUMBER}-${IOS_BUILD_ATTEMPT}"
NOTES="$(mktemp)"
trap 'rm -f "$NOTES"' EXIT
printf '%s\n' \
  'SingGram iOS prerelease. Download SingGram-unsigned.ipa and re-sign with your own certificate.' \
  'Bundle ID: com.sing.singgram. Keep this ID when re-signing.' \
  "Source commit: $IOS_BUILD_SHA" \
  "Build: $IOS_BUILD_URL" > "$NOTES"

if gh release view "$TAG" --repo "$GITHUB_REPOSITORY" >/dev/null 2>&1; then
  gh release upload "$TAG" "$IPA" --repo "$GITHUB_REPOSITORY" --clobber
  gh release edit "$TAG" --repo "$GITHUB_REPOSITORY" --prerelease --latest=false \
    --title "SingGram iOS build ${IOS_BUILD_NUMBER}.${IOS_BUILD_ATTEMPT}" --notes-file "$NOTES"
else
  gh release create "$TAG" "$IPA" --repo "$GITHUB_REPOSITORY" \
    --target "$IOS_BUILD_SHA" --prerelease --latest=false \
    --title "SingGram iOS build ${IOS_BUILD_NUMBER}.${IOS_BUILD_ATTEMPT}" --notes-file "$NOTES"
fi
printf 'Download: https://github.com/%s/releases/download/%s/SingGram-unsigned.ipa\n' "$GITHUB_REPOSITORY" "$TAG"
