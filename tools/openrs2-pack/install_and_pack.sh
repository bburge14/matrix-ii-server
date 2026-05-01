#!/usr/bin/env bash
# One-shot installer + runner for the OpenRS2 flat-to-disk packer.
#
# What this does:
#   1. Drops two .kt files into your ~/openrs2 clone (FlatDiskPackCommand.kt
#      and the patched CacheCommand.kt that registers it).
#   2. Rebuilds cache-cli via gradle.
#   3. Runs the pack command on your flat-format cache.
#   4. Verifies the output is real packed dat2/idx files.
#
# Prerequisites:
#   - ~/openrs2 already exists and was built once (gradlew :archive:installDist
#     and :cache-cli:installDist worked).
#   - Java 17+ available (OpenRS2 requires it).
#
# Usage from this directory:
#   ./install_and_pack.sh                                                # default paths
#   ./install_and_pack.sh ~/matrix/876_packed/cache  ~/matrix/876_repacked
#
# After it finishes successfully, point Settings.CACHE_PATH_PRIMARY at the
# output directory (or move the output to whatever path your server expects).

set -e

INPUT="${1:-$HOME/matrix/876_packed/cache}"
OUTPUT="${2:-$HOME/matrix/876_repacked}"
OPENRS2_DIR="${OPENRS2_DIR:-$HOME/openrs2}"

if [ ! -d "$OPENRS2_DIR" ]; then
    echo "ERROR: OpenRS2 source dir not found at $OPENRS2_DIR"
    echo "Set OPENRS2_DIR env var or clone it: git clone https://github.com/openrs2/openrs2.git ~/openrs2"
    exit 1
fi

CLI_KOTLIN_DIR="$OPENRS2_DIR/cache-cli/src/main/kotlin/org/openrs2/cache/cli"
if [ ! -d "$CLI_KOTLIN_DIR" ]; then
    echo "ERROR: $CLI_KOTLIN_DIR doesn't exist - is your OpenRS2 clone complete?"
    exit 1
fi

if [ ! -d "$INPUT" ]; then
    echo "ERROR: input flat-format cache dir not found: $INPUT"
    exit 1
fi

# Resolve absolute path of the script's own dir so we always copy from the right spot
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "[pack] OpenRS2 source: $OPENRS2_DIR"
echo "[pack] Input (flat):   $INPUT"
echo "[pack] Output (disk):  $OUTPUT"
echo

# 1) Drop the two files into the OpenRS2 source tree
echo "[pack] Installing FlatDiskPackCommand.kt + patched CacheCommand.kt..."
cp "$SCRIPT_DIR/FlatDiskPackCommand.kt" "$CLI_KOTLIN_DIR/FlatDiskPackCommand.kt"
cp "$SCRIPT_DIR/CacheCommand.kt"        "$CLI_KOTLIN_DIR/CacheCommand.kt"

# 2) Rebuild
echo "[pack] Rebuilding cache-cli (couple minutes)..."
cd "$OPENRS2_DIR"
./gradlew :cache-cli:installDist

CLI_BIN="$OPENRS2_DIR/cache-cli/build/install/cache-cli/bin/cache-cli"
if [ ! -x "$CLI_BIN" ]; then
    echo "ERROR: build did not produce $CLI_BIN"
    exit 1
fi

# 3) Sanity-check the new pack subcommand registered
echo
echo "[pack] CLI available subcommands:"
"$CLI_BIN" --help

if ! "$CLI_BIN" --help 2>&1 | grep -q "^\s*pack\b"; then
    echo "WARNING: 'pack' subcommand isn't listed - registration might have failed"
    echo "Continuing anyway in case the help formatter just hides it..."
fi

# 4) Run the pack
echo
echo "[pack] Running: $CLI_BIN pack $INPUT $OUTPUT"
mkdir -p "$OUTPUT"
"$CLI_BIN" pack "$INPUT" "$OUTPUT"

# 5) Verify
echo
echo "[pack] Output directory contents:"
ls -la "$OUTPUT" | head -20

DAT2_SIZE=$(stat -c%s "$OUTPUT/main_file_cache.dat2" 2>/dev/null || echo 0)
IDX_COUNT=$(ls "$OUTPUT"/main_file_cache.idx* 2>/dev/null | wc -l)

echo
if [ "$DAT2_SIZE" -gt 1000000 ] && [ "$IDX_COUNT" -ge 5 ]; then
    echo "[pack] SUCCESS: dat2=${DAT2_SIZE} bytes, idx files=${IDX_COUNT}"
    echo
    echo "Next: point Settings.CACHE_PATH_PRIMARY at $OUTPUT"
    echo "(or move the contents into ~/matrix/876_packed/)"
else
    echo "[pack] WARNING: output looks too small/empty"
    echo "       dat2=${DAT2_SIZE} bytes, idx files=${IDX_COUNT}"
    echo "       Something likely went wrong. Check the [pack] log lines above."
fi
