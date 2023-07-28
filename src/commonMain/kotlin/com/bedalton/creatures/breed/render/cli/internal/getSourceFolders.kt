package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.app.exitNativeWithError
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.nullIfEmpty
import com.bedalton.common.util.pathSeparator
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.vfs.ERROR_CODE__BAD_INPUT_FILE
import com.bedalton.vfs.LocalFileSystem
import com.bedalton.vfs.expandTildeIfNecessary
import com.bedalton.vfs.globFiles


/**
 * Get source folders from CLI source arguments
 */
internal suspend fun getSourceFolders(
    sources: List<String>,
    currentWorkingDirectory: String?
): List<String> {

    sources.filter { it.startsWith('-') }
        .filter {
            Log.iIf(LOG_DEBUG) { "Checking if valid folder: $it" }
            val path = it.expandTildeIfNecessary().stripDotSlash()
            val absolutePath = if (PathUtil.isAbsolute(path)) {
                path
            } else if (currentWorkingDirectory != null) {
                PathUtil.combine(currentWorkingDirectory, path)
            } else {
                exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE) {
                    "Failed to get current working directory for non-absolute paths"
                }
            }
            try {
                !LocalFileSystem!!.fileExists(absolutePath)
            } catch (e: Exception) {
                exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE, "Failed to check file existence;${e.formatted()}")
            }
        }
        .nullIfEmpty()
        ?.also {
            exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE) {
                "Invalid source directories found: [\n\t${it.joinToString("\n\t")}\n]\n " +
                        "Were these meant to be options? Check prefix dashes and option spelling\n"
            }
        }

    // Get all passed in paths qualified
    val rawPaths = sources
        .map { aPath ->
            val path = aPath.expandTildeIfNecessary().stripDotSlash()
            if (PathUtil.isAbsolute(path)) {
                path
            } else if (currentWorkingDirectory == null) {
                exitWithError(
                    ERROR_CODE__BAD_INPUT_FILE,
                    "Failed to get current working directory, and not all paths are absolute; Path: $aPath"
                )
            } else {
                PathUtil.combine(currentWorkingDirectory, path)
            }
        }

    // Get concrete folders (i.e. before or without wildcard)
    val folders = rawPaths.mapNotNull { path ->
        if (path.contains('[') || path.contains('*') || path.contains('{')) {
            val parts = path.split("[/\\\\]+")
            val firstWildCard = parts.indexOfFirst { it.contains('[') || it.contains('*') || it.contains('{') }
            if (firstWildCard > 0) {
                parts.slice(0 until firstWildCard).joinToString(pathSeparator)
            } else {
                null
            }
        } else {
            path
        }
    }

    Log.iIf(LOG_VERBOSE) {
        "Folders:\n\t-${folders.joinToString("\n\t-")}"
    }

    val localVFS = LocalFileSystem
        ?: exitNativeWithError(1, "File access without scope is not yet implemented")

    // Glob the wildcard paths
    val globResult = rawPaths.flatMap { path ->
        if (path.contains('[') || path.contains('*') || path.contains('{')) {
            localVFS.globFiles(path)
        } else {
            emptyList()
        }
    }


    Log.iIf(LOG_VERBOSE) {
        "GlobResults: \n\t-${globResult.joinToString("\n\t-")}"
    }

    return globResult + folders
}
