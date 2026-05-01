package org.openrs2.cache.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import io.netty.buffer.ByteBufAllocator
import org.openrs2.buffer.use
import org.openrs2.cache.CacheModule
import org.openrs2.cache.FlatFileStore
import org.openrs2.cache.Store
import org.openrs2.inject.CloseableInjector
import java.nio.file.Files

/**
 * Convert OpenRS2 flat-file format (loose <archive>.dat blobs in numbered
 * subdirs, one dir per index) to JS5 disk format (main_file_cache.dat2 +
 * main_file_cache.idx*). Inverse of OpenNxtStore.unpack-equivalent.
 *
 * Usage:
 *   archive cache pack <flat-input-dir> <disk-output-dir>
 *
 * Both directories must exist; the output is created if missing.
 */
public class FlatDiskPackCommand : CliktCommand(name = "pack") {
    private val input by argument().path(mustExist = true, canBeFile = false, mustBeReadable = true)
    private val output by argument().path(canBeFile = false)

    override fun run() {
        CloseableInjector(Guice.createInjector(CacheModule)).use { injector ->
            val alloc = injector.getInstance(ByteBufAllocator::class.java)
            Files.createDirectories(output)

            FlatFileStore.open(input, alloc).use { src ->
                Store.open(output, alloc).use { dst ->
                    val archives = src.list()
                    var copied = 0L
                    var archIdx = 0
                    for (archive in archives) {
                        archIdx++
                        dst.create(archive)
                        val groups = src.list(archive)
                        for (group in groups) {
                            src.read(archive, group).use { buf ->
                                dst.write(archive, group, buf)
                                copied++
                                if (copied % 5000 == 0L) {
                                    System.err.println("[pack] $copied groups (idx $archIdx/${archives.size}, archive $archive)")
                                }
                            }
                        }
                    }
                    System.err.println("[pack] DONE - $copied groups across $archIdx indexes")
                }
            }
        }
    }
}
