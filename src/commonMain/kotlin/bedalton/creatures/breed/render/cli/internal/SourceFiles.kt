package bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.coroutines.LockingDataCache
import com.bedalton.common.structs.UnsafeDataCache
import com.bedalton.log.Log
import com.bedalton.vfs.FileSystem

internal data class SourceFiles(
    val fs: FileSystem,
    val sources: List<String>,
    private val genomeFiles: List<String>?,
    private val genomeFilter: String?
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

    suspend fun getGenomePath(): String? {
        return mGenomePath.getData()
    }
}
