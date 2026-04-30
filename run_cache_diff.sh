#!/usr/bin/env bash
# Run the cache delta dumper. Compares the 876 base cache against the 900+ DLC cache.
#
# Usage:
#   ./run_cache_diff.sh                                      # uses default paths
#   ./run_cache_diff.sh /path/to/876_cache /path/to/dlc      # custom paths
#   ./run_cache_diff.sh /path/to/876_cache /path/to/dlc /tmp/out.txt
#
# Output: data/dump/cache_comparison.txt (unless arg 3 overrides)
#
# Where to put this script:
#   This must live in the SAME DIRECTORY as src/, data/, run_game.sh
#   (i.e. the matrix-ii-server repo root). Running it from anywhere else
#   will fail with "package com.alex.store does not exist".

set -e
cd "$(dirname "$0")"

# Sanity check we're at the repo root.
if [ ! -d src/com/rs ] || [ ! -f data/libs/FileStore.jar ]; then
  echo "ERROR: this script must be run from the matrix-ii-server repo root."
  echo "Expected to find: src/com/rs/  and  data/libs/FileStore.jar"
  echo "Current dir: $(pwd)"
  echo "Move run_cache_diff.sh next to run_game.sh and try again."
  exit 1
fi

mkdir -p bin data/dump

# Recompile the full source tree if any .java is newer than the utility class.
# We compile EVERYTHING because com.rs.cache.loaders.* pulls in dozens of
# transitive deps (Skills, Equipment, Combat, etc) and trying to cherry-pick
# files will fail with cascading 'cannot find symbol' errors.
NEED_COMPILE=0
if [ ! -f bin/com/rs/tools/CacheDiffUtility.class ]; then
  NEED_COMPILE=1
elif [ src/com/rs/tools/CacheDiffUtility.java -nt bin/com/rs/tools/CacheDiffUtility.class ]; then
  NEED_COMPILE=1
fi

if [ "$NEED_COMPILE" = "1" ]; then
  echo "[run_cache_diff] compiling full source tree (one-time, ~30s)..."
  # Generate a list of every .java file in src/. xargs handles the long list
  # without blowing past command-line length limits.
  find src -name "*.java" -type f > /tmp/cache_diff_sources.txt
  echo "[run_cache_diff] $(wc -l < /tmp/cache_diff_sources.txt) source files"

  # -encoding Cp1252 because two source files contain non-UTF-8 bytes from the
  # original RSPS leak (Bank.java, Summoning.java) and would crash javac otherwise.
  if ! javac -encoding Cp1252 -d bin -cp "data/libs/*:src" \
        -source 1.8 -target 1.8 -Xlint:none -nowarn \
        @/tmp/cache_diff_sources.txt 2>&1 | grep -v -E "^Note:|deprecated|deprecation|unchecked"; then
    : # grep returns non-zero if everything is filtered, that's fine
  fi
  rm -f /tmp/cache_diff_sources.txt

  if [ ! -f bin/com/rs/tools/CacheDiffUtility.class ]; then
    echo "ERROR: compilation failed - bin/com/rs/tools/CacheDiffUtility.class not produced"
    echo "Re-run without the redirect above to see all errors."
    exit 1
  fi
  echo "[run_cache_diff] compile OK"
fi

# Run with 2GB heap. Bump to -Xmx4G if it OOMs on the larger cache.
java -Xmx2G -cp "bin:data/libs/*" com.rs.tools.CacheDiffUtility "$@"
