#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OFFICIAL_DIR="$ROOT_DIR/official-android"
UPSTREAM_URL="${SINGGRAM_UPSTREAM_URL:-https://github.com/DrKLO/Telegram.git}"
UPSTREAM_REMOTE="${SINGGRAM_UPSTREAM_REMOTE:-upstream}"
UPSTREAM_REF="${1:-master}"
ALLOW_DIRTY="${SINGGRAM_ALLOW_DIRTY:-false}"

if [[ ! -d "$OFFICIAL_DIR/.git" ]]; then
  echo "Missing official-android git checkout at $OFFICIAL_DIR"
  exit 1
fi

cd "$OFFICIAL_DIR"

if [[ "$ALLOW_DIRTY" != "true" && -n "$(git status --porcelain)" ]]; then
  echo "official-android has local changes. Commit/stash them first, or set SINGGRAM_ALLOW_DIRTY=true."
  git status --short
  exit 1
fi

if ! git remote get-url "$UPSTREAM_REMOTE" >/dev/null 2>&1; then
  git remote add "$UPSTREAM_REMOTE" "$UPSTREAM_URL"
fi

echo "Fetching $UPSTREAM_REMOTE from $UPSTREAM_URL"
git fetch --tags "$UPSTREAM_REMOTE"

TARGET_REF="$UPSTREAM_REMOTE/$UPSTREAM_REF"
if ! git rev-parse --verify --quiet "$TARGET_REF" >/dev/null; then
  TARGET_REF="$UPSTREAM_REF"
fi

if ! git rev-parse --verify --quiet "$TARGET_REF" >/dev/null; then
  echo "Cannot find upstream ref: $UPSTREAM_REF"
  exit 1
fi

SAFE_REF="$(echo "$UPSTREAM_REF" | tr '/:@ ' '----')"
SYNC_BRANCH="singgram/sync-${SAFE_REF}-$(date +%Y%m%d-%H%M)"
BASE_BRANCH="$(git branch --show-current)"

echo "Creating sync branch: $SYNC_BRANCH"
git checkout -b "$SYNC_BRANCH"

echo "Merging $TARGET_REF into $SYNC_BRANCH"
if git merge --no-ff --no-commit "$TARGET_REF"; then
  echo
  echo "Upstream merged without conflicts."
  echo "Next:"
  echo "  1. Review changes and run scripts/build_official_android.sh"
  echo "  2. Commit the merge on $SYNC_BRANCH"
  echo "  3. Merge $SYNC_BRANCH back into $BASE_BRANCH"
else
  echo
  echo "Merge has conflicts. Resolve them, then run:"
  echo "  git status"
  echo "  scripts/build_official_android.sh"
  echo "  git commit"
fi
