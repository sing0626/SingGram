#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OFFICIAL_DIR="$ROOT_DIR/official-android"
ENV_FILE="$ROOT_DIR/telegram_credentials.env"
GRADLE_TASK="${SINGGRAM_OFFICIAL_TASK:-:TMessagesProj_App:assembleArm64Release}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT_DIR/.gradle-sandbox}"
GRADLE_ARGS=(--parallel --build-cache)

if [[ "${SINGGRAM_GRADLE_NO_DAEMON:-false}" == "true" ]]; then
  GRADLE_ARGS+=(--no-daemon)
fi

if [[ ! -d "$OFFICIAL_DIR" ]]; then
  echo "Missing official-android. Run the official Android fork bootstrap first."
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  set -a
  source "$ENV_FILE"
  set +a
fi

if [[ -z "${TELEGRAM_API_ID:-}" || -z "${TELEGRAM_API_HASH:-}" ]]; then
  echo "TELEGRAM_API_ID and TELEGRAM_API_HASH are required."
  echo "Set them in telegram_credentials.env or export them before running this script."
  exit 1
fi

cd "$OFFICIAL_DIR"
export GRADLE_USER_HOME
TELEGRAM_API_ID="$TELEGRAM_API_ID" \
TELEGRAM_API_HASH="$TELEGRAM_API_HASH" \
SINGGRAM_FORCE_LIQUID_GLASS="${SINGGRAM_FORCE_LIQUID_GLASS:-true}" \
./gradlew "${GRADLE_ARGS[@]}" "$GRADLE_TASK"
