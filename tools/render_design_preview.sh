#!/usr/bin/env bash
# Offline SVG specimens from the actual numeric/icon renderers, using test-only Canvas adapters.
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
preview_out="${1:-design-review/preview}"
mkdir -p "$preview_out"
preview_classes=$(mktemp -d "${TMPDIR:-/tmp}/freemytv-preview.XXXXXX")
trap 'rm -rf "$preview_classes"' EXIT
"$JAVAC" -d "$preview_classes" tools/visual-preview/android/graphics/*.java \
  app/src/io/freemytv/Theme.java app/src/io/freemytv/DotMatrix.java \
  app/src/io/freemytv/DotDisplay.java app/src/io/freemytv/DrawerSymbols.java \
  tools/visual-preview/io/freemytv/Preview.java
"$JAVA" -cp "$preview_classes" io.freemytv.Preview "$preview_out"
