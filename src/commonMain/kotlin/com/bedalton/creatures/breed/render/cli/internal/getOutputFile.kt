package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.app.exitNativeWithError
import com.bedalton.common.util.PathUtil
import com.bedalton.vfs.ERROR_CODE__BAD_OUTPUT_FILE
import com.bedalton.vfs.LocalFileSystem
import com.bedalton.vfs.expandTildeIfNecessary

internal suspend fun getOutputFile(out: String, currentWorkingDirectory: String?): String {


    // Get out base path. Could be a file or a folder
    val outWithExpandedTilde = out.expandTildeIfNecessary().stripDotSlash()
    // Get path while expanding tilde, as this failed on Ubuntu
    val outActual = if (!PathUtil.isAbsolute(outWithExpandedTilde)) {
        if (currentWorkingDirectory == null) {
            throw Exception("Failed to get current working directory")
        }
        PathUtil.combine(currentWorkingDirectory, outWithExpandedTilde)
    } else {
        outWithExpandedTilde
    }

    if (LocalFileSystem!!.isDirectory(outActual)) {
        exitNativeWithError(ERROR_CODE__BAD_OUTPUT_FILE, "Output file is required. Found: $out")
    }
    return outActual
}