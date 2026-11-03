#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOS_DIR="$ROOT_DIR/official-ios"
LOCK_FILE="$IOS_DIR/singgram-submodules.lock"

if [[ ! -f "$LOCK_FILE" ]]; then
  echo "Missing $LOCK_FILE"
  exit 1
fi

while IFS=$'\t' read -r rel_path url commit; do
  [[ -z "${rel_path// }" ]] && continue
  [[ "$rel_path" == \#* ]] && continue
  dest="$IOS_DIR/$rel_path"
  echo "Fetching $rel_path @ $commit"
  rm -rf "$dest"
  mkdir -p "$dest"
  git init -q "$dest"
  git -C "$dest" remote add origin "$url"
  git -C "$dest" fetch --depth 1 origin "$commit"
  git -C "$dest" checkout --detach --quiet FETCH_HEAD
  if [[ -f "$dest/.gitmodules" ]]; then
    git -C "$dest" submodule update --init --recursive --depth 1 \
      || git -C "$dest" submodule update --init --recursive
  fi
  while IFS= read -r -d '' gitdir; do
    rm -rf "$gitdir"
  done < <(find "$dest" -name .git \( -type d -o -type f \) -prune -print0)
done < "$LOCK_FILE"

echo "iOS submodules are checked out under $IOS_DIR"
