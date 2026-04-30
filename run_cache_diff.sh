#!/usr/bin/env bash
# Run the cache delta dumper. Compares the 876 base cache against the 900+ DLC cache.
#
# Usage:
#   ./run_cache_diff.sh                                      # uses default paths
#   ./run_cache_diff.sh /path/to/876_cache /path/to/dlc      # custom paths
#   ./run_cache_diff.sh /path/to/876_cache /path/to/dlc /tmp/out.txt
#
# Output: data/dump/cache_comparison.txt (unless arg 3 overrides)
set -e
cd "$(dirname "$0")"
mkdir -p data/dump

# Make sure the utility is compiled. Quick incremental rebuild only if needed.
if [ ! -f bin/com/rs/tools/CacheDiffUtility.class ] \
   || [ src/com/rs/tools/CacheDiffUtility.java -nt bin/com/rs/tools/CacheDiffUtility.class ]; then
  echo "[run_cache_diff] compiling CacheDiffUtility..."
  mkdir -p bin
  javac -encoding Cp1252 -d bin -cp "data/libs/*:src" \
        -source 1.8 -target 1.8 -Xlint:none -nowarn \
        src/com/rs/tools/CacheDiffUtility.java \
        src/com/rs/cache/Cache.java \
        src/com/rs/cache/loaders/ItemDefinitions.java \
        src/com/rs/cache/loaders/ObjectDefinitions.java \
        src/com/rs/cache/loaders/NPCDefinitions.java
fi

java -Xmx2G -cp "bin:data/libs/*" com.rs.tools.CacheDiffUtility "$@"
