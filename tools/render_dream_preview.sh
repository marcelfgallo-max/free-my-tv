#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ]; then export JAVA_HOME=$(/usr/libexec/java_home -v 17); fi
preview_out="${1:-design-review/phase7-preview}"
mkdir -p "$preview_out"
preview_classes=$(mktemp -d "${TMPDIR:-/tmp}/freemytv-dream-preview.XXXXXX")
trap 'rm -rf "$preview_classes"' EXIT
"$JAVA_HOME/bin/javac" -d "$preview_classes" tools/visual-preview/android/graphics/*.java \
 app/src/io/freemytv/Theme.java app/src/io/freemytv/DotMatrix.java \
 app/src/io/freemytv/DotDisplay.java app/src/io/freemytv/DrawerSymbols.java \
 tools/visual-preview/io/freemytv/Preview.java tools/visual-preview/io/freemytv/DreamPreview.java
"$JAVA_HOME/bin/java" -cp "$preview_classes" io.freemytv.DreamPreview "$preview_out"
