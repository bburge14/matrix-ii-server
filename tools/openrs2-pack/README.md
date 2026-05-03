# OpenRS2 Flat -> Disk Pack

Tool for converting a cache in OpenRS2 flat-file format (loose
`<archive>.dat` blobs in numbered subdirectories) to JS5 disk format
(`main_file_cache.dat2` + `main_file_cache.idx*`) that the Matrix II
server can load.

## Files

| File | Purpose |
|------|---------|
| `FlatDiskPackCommand.kt` | New CLI subcommand `pack` |
| `CacheCommand.kt` | Patched to register the new command |
| `install_and_pack.sh` | One-shot installer + runner |

## Prerequisites

OpenRS2 cloned to `~/openrs2` and built at least once:

```bash
cd ~ && git clone https://github.com/openrs2/openrs2.git
cd openrs2 && ./gradlew :archive:installDist :cache-cli:installDist
```

Java 17+ required (OpenRS2 dependency).

## Run

```bash
cd ~/matrix/Server/tools/openrs2-pack
chmod +x install_and_pack.sh
./install_and_pack.sh
```

That uses default paths:
- Input:  `~/matrix/876_packed/cache` (your flat-format dir)
- Output: `~/matrix/876_repacked`

To override:
```bash
./install_and_pack.sh /path/to/flat-input /path/to/disk-output
```

## After successful pack

Either move the output into the path Settings expects:

```bash
rm -rf ~/matrix/876_packed/{main_file_cache.*,cache,cache.zip,flat-file}
mv ~/matrix/876_repacked/* ~/matrix/876_packed/
```

Or update `src/com/rs/Settings.java`:

```java
public static final String CACHE_PATH_PRIMARY =
    System.getProperty("user.home") + "/matrix/876_repacked/";
```

Recompile + restart the server. The smoke test in `Cache.init()` will
accept it and you'll boot on 876.
