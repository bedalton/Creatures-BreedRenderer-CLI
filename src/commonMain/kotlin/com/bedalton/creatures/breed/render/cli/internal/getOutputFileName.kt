package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.util.PathUtil
import com.bedalton.vfs.FileSystem

private const val DEFAULT_FILE_NAME = "render"

internal suspend fun getOutputFileName(
    localVFS: FileSystem,
    out: String,
    i: Int,
    isMultiPose: Boolean,
    increment: Boolean,
    previousFiles: MutableList<String>
): String {
    if (!isMultiPose && !increment) {
        return if (out.lowercase().endsWith(".png")) {
            out
        } else if (localVFS.isDirectory(out)) {
            PathUtil.combine(out, DEFAULT_FILE_NAME)
        } else {
            "$out.png"
        }
    }
    val prefix = if (out.endsWith(".png")) {
            PathUtil.combine(
                PathUtil.getWithoutLastPathComponent(out) ?: "",
                (PathUtil.getFileNameWithoutExtension(out) ?: DEFAULT_FILE_NAME)
            )
        } else {
            if (localVFS.isDirectory(out)) {
                PathUtil.combine(out, DEFAULT_FILE_NAME)
            } else {
                out
            }
        }

    var temp: String
    var j = i
    do {
        temp = "$prefix.${j++}.png"
    } while ((increment && localVFS.fileExists(temp)) || temp in previousFiles)
    return temp
}