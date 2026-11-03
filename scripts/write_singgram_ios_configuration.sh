#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${SINGGRAM_IOS_CONFIG_PATH:-$ROOT_DIR/official-ios/build-system/singgram-configuration.json}"

if [[ -z "${TELEGRAM_API_ID:-}" || -z "${TELEGRAM_API_HASH:-}" ]]; then
  echo "Set TELEGRAM_API_ID and TELEGRAM_API_HASH before writing the iOS configuration." >&2
  exit 1
fi

if [[ -z "${SINGGRAM_IOS_TEAM_ID:-}" ]]; then
  echo "Set SINGGRAM_IOS_TEAM_ID. The unsigned Actions build uses C67CF9S4VU so the bundled fake profiles match." >&2
  exit 1
fi

export SINGGRAM_IOS_BUNDLE_ID="${SINGGRAM_IOS_BUNDLE_ID:-com.sing.singgram}"
export SINGGRAM_IOS_APP_CENTER_ID="${SINGGRAM_IOS_APP_CENTER_ID:-0}"
export SINGGRAM_IOS_INTERNAL_BUILD="${SINGGRAM_IOS_INTERNAL_BUILD:-false}"
export SINGGRAM_IOS_APPSTORE_BUILD="${SINGGRAM_IOS_APPSTORE_BUILD:-false}"
export SINGGRAM_IOS_APPSTORE_ID="${SINGGRAM_IOS_APPSTORE_ID:-0}"
export SINGGRAM_IOS_URL_SCHEME="${SINGGRAM_IOS_URL_SCHEME:-tg}"
export SINGGRAM_IOS_PREMIUM_PRODUCT="${SINGGRAM_IOS_PREMIUM_PRODUCT:-}"
export SINGGRAM_IOS_ENABLE_SIRI="${SINGGRAM_IOS_ENABLE_SIRI:-false}"
export SINGGRAM_IOS_ENABLE_ICLOUD="${SINGGRAM_IOS_ENABLE_ICLOUD:-false}"
export SINGGRAM_IOS_CONFIG_PATH="$OUT"

python3 - <<'PY'
import json
import os

def flag(name):
    return os.environ[name].lower() == "true"

payload = {
    "bundle_id": os.environ["SINGGRAM_IOS_BUNDLE_ID"],
    "api_id": os.environ["TELEGRAM_API_ID"],
    "api_hash": os.environ["TELEGRAM_API_HASH"],
    "team_id": os.environ["SINGGRAM_IOS_TEAM_ID"],
    "app_center_id": os.environ["SINGGRAM_IOS_APP_CENTER_ID"],
    "is_internal_build": os.environ["SINGGRAM_IOS_INTERNAL_BUILD"],
    "is_appstore_build": os.environ["SINGGRAM_IOS_APPSTORE_BUILD"],
    "appstore_id": os.environ["SINGGRAM_IOS_APPSTORE_ID"],
    "app_specific_url_scheme": os.environ["SINGGRAM_IOS_URL_SCHEME"],
    "premium_iap_product_id": os.environ["SINGGRAM_IOS_PREMIUM_PRODUCT"],
    "enable_siri": flag("SINGGRAM_IOS_ENABLE_SIRI"),
    "enable_icloud": flag("SINGGRAM_IOS_ENABLE_ICLOUD"),
}
path = os.environ["SINGGRAM_IOS_CONFIG_PATH"]
with open(path, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, indent=2)
    handle.write("\n")
print("Wrote", path)
PY
