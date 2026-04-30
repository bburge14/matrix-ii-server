#!/usr/bin/env bash
# Convert OpenRS2 loose-file cache (numbered subdirs of .dat blobs) into
# the packed main_file_cache.dat2 + main_file_cache.idx* format that the
# alex/store FileStore library and the Matrix II server can actually load.
#
# Usage:
#   ./run_cache_repack.sh                                                 # default paths
#   ./run_cache_repack.sh ~/matrix/876_cache/cache/ ~/matrix/876_packed/  # explicit
#
# After this finishes, run the diff utility against the packed dir:
#   ./run_cache_diff.sh ~/matrix/830_cache/ ~/matrix/876_packed/ data/dump/830_to_876.txt
#
# Must be run from the matrix-ii-server repo root (where src/ + data/libs/ are).

set -e
cd "$(dirname "$0")"

if [ ! -d src/com/rs ] || [ ! -f data/libs/FileStore.jar ]; then
  echo "ERROR: run from repo root (e.g. ~/matrix/Server/)"
  exit 1
fi

INPUT="${1:-$HOME/matrix/876_cache/cache/}"
OUTPUT="${2:-$HOME/matrix/876_packed/}"

mkdir -p bin "$OUTPUT"

# Compile if needed. Same approach as run_cache_diff.sh - whole tree.
if [ ! -f bin/com/rs/tools/CacheRepacker.class ] \
   || [ src/com/rs/tools/CacheRepacker.java -nt bin/com/rs/tools/CacheRepacker.class ]; then
  echo "[run_cache_repack] compiling full source tree (one-time, ~30s)..."
  find src -name "*.java" -type f > /tmp/cache_repack_sources.txt
  javac -encoding Cp1252 -d bin -cp "data/libs/*:src" \
        -source 1.8 -target 1.8 -Xlint:none -nowarn \
        @/tmp/cache_repack_sources.txt 2>&1 | grep -v -E "^Note:|deprecated|deprecation|unchecked" || true
  rm -f /tmp/cache_repack_sources.txt
  if [ ! -f bin/com/rs/tools/CacheRepacker.class ]; then
    echo "ERROR: compile failed"
    exit 1
  fi
fi

java -Xmx4G -cp "bin:data/libs/*" com.rs.tools.CacheRepacker "$INPUT" "$OUTPUT"
