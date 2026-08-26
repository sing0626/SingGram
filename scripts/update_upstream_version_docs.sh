#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "Usage: $0 <version> <version-code> <commit>" >&2
  exit 64
fi

version="$1"
version_code="$2"
commit="$3"

if [[ ! "$version" =~ ^[0-9][0-9A-Za-z._-]*$ || ! "$version_code" =~ ^[0-9]+$ || ! "$commit" =~ ^[0-9A-Fa-f]{7,64}$ ]]; then
  echo "Invalid upstream version metadata." >&2
  exit 65
fi

SG_VERSION="$version" SG_VERSION_CODE="$version_code" SG_COMMIT="$commit" perl -0pi -e '
  my $version = $ENV{SG_VERSION};
  my $code = $ENV{SG_VERSION_CODE};
  s{^(- Current imported upstream: Telegram Android `)[^`]+(` / version code `)[^`]+(`)$}{$1 . $version . $2 . $code . $3}gme;
' README.md

SG_VERSION="$version" SG_VERSION_CODE="$version_code" SG_COMMIT="$commit" perl -0pi -e '
  my $version = $ENV{SG_VERSION};
  my $code = $ENV{SG_VERSION_CODE};
  my $commit = $ENV{SG_COMMIT};
  s{^(- Imported upstream metadata: `)[^`]+(` / Telegram Android `)[^`]+(` / version code `)[^`]+(`)$}{$1 . $commit . $2 . $version . $3 . $code . $4}gme;
' docs/official-android-fork.md
