package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.exports.minimal.ExportRenderData
import com.bedalton.app.exitNativeWithError
import com.bedalton.common.coroutines.LockingDataCache
import com.bedalton.creatures.exports.minimal.ExportParser
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.vfs.FileSystem

internal data class SourceFiles(
    val fs: FileSystem,
    val sources: List<String>,
    private val genomeFiles: List<String>?,
    private val genomeFilter: String?,
    private val exportPath: String?,
) {
    private val mGenomePath = LockingDataCache<String> lazy@{
        val file = genomeFiles?.singleOrNull()
            ?: return@lazy null

        if (genomeFilter == null) {
            return@lazy file
        }
        if (!fs.isDirectory(file)) {
            return@lazy file
        }
        val filter = genomeFilter.toRegex(RegexOption.IGNORE_CASE)
        fs.findFirst(file, refresh = true, recursive = true, filter = filter::matches)
    }

    val exportData: LockingDataCache<List<ExportRenderData>> = LockingDataCache {
        if (exportPath != null) {
            Log.i { "Parsing export in data cache" }
            try {
                ExportParser.parseExport(fs, exportPath)
            } catch (e: Exception) {
                exitNativeWithError(1, "Failed to parse export; ${e.formatted()}")
            }
        } else {
            Log.iIf(LOG_VERBOSE) { "No export path in SourceFiles" }
            null
        }
    }

    suspend fun getGenomePath(): String? {
        return mGenomePath.getData()
    }
}
