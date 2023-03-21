package bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.PathUtil
import com.bedalton.vfs.FileSystem

internal suspend fun getIncrementalFileStart(localVFS: FileSystem, out: String, poseCount: Int): Int {
    val prefix = if (out.endsWith("png")) {
        PathUtil.combine(
            PathUtil.getWithoutLastPathComponent(out) ?: "",
            (PathUtil.getFileNameWithoutExtension(out) ?: "render")
        )
    } else {
        out
    }
    var i = 0
    var okay: Boolean
    var temp: String
    do {
        okay = true
        for (j in 0 until poseCount) {
            temp = "$prefix.${(i + j)}.png"
            if (localVFS.fileExists(temp)) {
                okay = false
                break
            }
        }
        if (okay) {
            break
        } else {
            i++
        }
    } while (!okay)
    return i
}