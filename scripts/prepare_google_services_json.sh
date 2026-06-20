#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 path/to/google-services.json [package.name]" >&2
  exit 2
fi

GOOGLE_SERVICES_JSON="$1"
PACKAGE_NAME="${2:-com.sing.singgram}"

if [[ ! -f "$GOOGLE_SERVICES_JSON" ]]; then
  echo "Missing google-services.json: $GOOGLE_SERVICES_JSON" >&2
  exit 1
fi

TMP_FILE="$(mktemp)"
trap 'rm -f "$TMP_FILE"' EXIT

jq --arg packageName "$PACKAGE_NAME" '
  def package_name: .client_info.android_client_info.package_name;

  if ((.client // []) | map(select(package_name == $packageName)) | length) > 0 then
    .
  else
    (((.client // []) | map(select(package_name == "org.telegram.messenger")) | first)
      // ((.client // []) | first)) as $template
    | if $template == null then
        error("google-services.json has no client entries")
      else
        .client += [
          ($template | .client_info.android_client_info.package_name = $packageName)
        ]
      end
  end
' "$GOOGLE_SERVICES_JSON" > "$TMP_FILE"

mv "$TMP_FILE" "$GOOGLE_SERVICES_JSON"
trap - EXIT
