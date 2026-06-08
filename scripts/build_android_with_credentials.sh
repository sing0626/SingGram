#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/telegram_credentials.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing telegram_credentials.env"
  echo "Copy telegram_credentials.env.example to telegram_credentials.env and fill your values."
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

if [[ -z "${TELEGRAM_API_ID:-}" || -z "${TELEGRAM_API_HASH:-}" ]]; then
  echo "TELEGRAM_API_ID and TELEGRAM_API_HASH are required."
  exit 1
fi

flutter build apk --debug \
  --dart-define="TELEGRAM_API_ID=$TELEGRAM_API_ID" \
  --dart-define="TELEGRAM_API_HASH=$TELEGRAM_API_HASH"
