#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BUILD="$ROOT/build/dicttool"
CLASSES="$BUILD/classes"
LIBS="$BUILD/libs"
OUT_DIR="${OUT_DIR:-$ROOT/java/res/raw}"
if [ -z "${JAVAC:-}" ]; then
    if command -v javac >/dev/null 2>&1; then JAVAC=javac
    elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then JAVAC="$JAVA_HOME/bin/javac"
    else JAVAC="$(ls -d "$HOME"/.gradle/jdks/*/bin/javac /usr/lib/jvm/*/bin/javac 2>/dev/null | head -n1 || true)"
    fi
fi
[ -n "$JAVAC" ] || { echo "No javac found; set JAVAC or JAVA_HOME" >&2; exit 1; }
JAVA="${JAVA:-$(dirname "$JAVAC")/java}"
mkdir -p "$CLASSES" "$LIBS" "$OUT_DIR"

find_jar() { # <var> <maven path> <file name> <sha256>
    local var="$1" mpath="$2" name="$3" sha="$4" found=""
    if [ -n "${!var:-}" ] && [ -f "${!var}" ]; then echo "${!var}"; return; fi
    found="$(find "$HOME/.gradle/caches" "$HOME/.m2" -name "$name" 2>/dev/null | head -n1 || true)"
    if [ -z "$found" ]; then
        found="$LIBS/$name"
        if [ ! -f "$found" ]; then
            echo "Downloading $name from Maven Central" >&2
            curl -fsSL -o "$found" "https://repo1.maven.org/maven2/$mpath/$name"
        fi
    fi
    if ! echo "$sha  $found" | sha256sum -c --status; then
        echo "SHA-256 mismatch for $found" >&2; exit 1
    fi
    echo "$found"
}

JSR305="$(find_jar JSR305_JAR com/google/code/findbugs/jsr305/3.0.2 jsr305-3.0.2.jar \
    766ad2a0783f2687962c8ad74ceecc38a28b9f72a2d085ee438b7813e928d0c7)"
JUNIT="$(find_jar JUNIT_JAR junit/junit/4.13.2 junit-4.13.2.jar \
    8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3)"
CP="$JSR305:$JUNIT"

SRCLIST="$BUILD/sources.txt"
{
    find "$ROOT/tools/dicttool/src" "$ROOT/tools/dicttool/tests" \
         "$ROOT/tools/dicttool/compat" "$ROOT/common/src" \
         "$ROOT/java/src/com/android/inputmethod/latin/makedict" \
         "$ROOT/tests/src/com/android/inputmethod/latin/makedict" -name '*.java'
    for f in \
        java/src/com/android/inputmethod/latin/BinaryDictionary.java \
        java/src/com/android/inputmethod/latin/DicTraverseSession.java \
        java/src/com/android/inputmethod/latin/Dictionary.java \
        java/src/com/android/inputmethod/latin/NgramContext.java \
        java/src/com/android/inputmethod/latin/SuggestedWords.java \
        java/src/com/android/inputmethod/latin/settings/SettingsValuesForSuggestion.java \
        java/src/com/android/inputmethod/latin/utils/BinaryDictionaryUtils.java \
        java/src/com/android/inputmethod/latin/utils/CombinedFormatUtils.java \
        java/src/com/android/inputmethod/latin/utils/JniUtils.java \
        java/src/com/android/inputmethod/latin/define/DebugFlags.java \
        java/src/com/android/inputmethod/latin/define/DecoderSpecificConstants.java \
        tests/src/com/android/inputmethod/latin/utils/ByteArrayDictBuffer.java; do
        echo "$ROOT/$f"
    done
} | sort > "$SRCLIST"

echo "Compiling dicttool ($(wc -l < "$SRCLIST") sources)"
rm -rf "$CLASSES"; mkdir -p "$CLASSES"
"$JAVAC" -nowarn -encoding UTF-8 --release 17 -d "$CLASSES" -cp "$CP" @"$SRCLIST" 2>&1 \
    | grep -v '^Note:' || true
[ -f "$CLASSES/com/android/inputmethod/latin/dicttool/Dicttool.class" ] || { echo "dicttool build failed" >&2; exit 1; }

cat > "$BUILD/dicttool" <<EOF
exec "$JAVA" -Xmx4g -cp "$CLASSES:$CP" com.android.inputmethod.latin.dicttool.Dicttool "\$@"
EOF
chmod +x "$BUILD/dicttool"
echo "dicttool ready: $BUILD/dicttool"

[ "${1:-}" = "--tool-only" ] && exit 0

if [ $# -gt 0 ]; then
    LOCALES=("$@")
else
    LOCALES=()
    for f in "$ROOT"/dictionaries/*_wordlist.combined.gz; do
        b="$(basename "$f")"; LOCALES+=("${b%_wordlist.combined.gz}")
    done
fi

TMP="$BUILD/tmp"; mkdir -p "$TMP"
for loc in "${LOCALES[@]}"; do
    src="$ROOT/dictionaries/${loc}_wordlist.combined.gz"
    out="$OUT_DIR/main_$(echo "$loc" | tr '[:upper:]' '[:lower:]').dict"
    if [ -f "$out" ] && [ "${FORCE:-0}" != 1 ]; then
        echo "skip $loc (exists: ${out#$ROOT/}; FORCE=1 to rebuild)"; continue
    fi
    echo "makedict $loc -> ${out#$ROOT/}"
    gunzip -c "$src" > "$TMP/$loc.combined"
    "$BUILD/dicttool" makedict -t "${CODE_POINT_TABLE:-on}" -s "$TMP/$loc.combined" -d "$TMP/$loc.dict" > "$TMP/$loc.log" || { cat "$TMP/$loc.log" >&2; exit 1; }
    mv "$TMP/$loc.dict" "$out"
    rm -f "$TMP/$loc.combined"
done
