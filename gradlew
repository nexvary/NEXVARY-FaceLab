#!/usr/bin/env sh
set -eu
VERSION=8.10.2
EXPECTED_SHA256=31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26
CACHE="${HOME}/.gradle/nexvary-bootstrap"
ZIP="$CACHE/gradle-$VERSION-bin.zip"
DIR="$CACHE/gradle-$VERSION"
GRADLE="$DIR/bin/gradle"
if [ ! -x "$GRADLE" ]; then
  mkdir -p "$CACHE"
  echo "[NEXVARY FaceLab] Preparing Gradle $VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip"
  else
    echo "ERROR: curl or wget is required." >&2
    exit 1
  fi
  ACTUAL_SHA256=$(sha256sum "$ZIP" | cut -d ' ' -f 1)
  if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
    echo "ERROR: Gradle distribution checksum mismatch" >&2
    rm -f "$ZIP"
    exit 1
  fi
  unzip -oq "$ZIP" -d "$CACHE"
fi
exec "$GRADLE" "$@"
