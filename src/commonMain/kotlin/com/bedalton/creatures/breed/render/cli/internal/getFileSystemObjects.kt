package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.util.PathUtil
import com.bedalton.creatures.breed.render.support.pose.Pose
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.vfs.FileSystem
import com.bedalton.vfs.LocalFileSystem
import com.bedalton.vfs.ScopedFileSystem

internal suspend fun getFileSystemSupport(
    sourceFiles: SourceFiles,
    out: String,
    currentWorkingDirectory: String?,
    poses: List<Pair<Pose, String?>>,
    increment: Boolean,
    isMultiPose: Boolean,
): Pair<FileSystem, suspend (i: Int, String?) -> String> {

    val outQualified = getOutputFile(out, currentWorkingDirectory)

    val tempFileSystem = LocalFileSystem ?: sourceFiles.fs

    // Get start file number increment, for sequential files
    val startI = if (increment) {
        Log.iIf(LOG_VERBOSE) { "Getting incremental start" }
        getIncrementalFileStart(tempFileSystem, outQualified, poses.count { it.second == null })
    } else {
        0
    }

    Log.iIf(LOG_DEBUG) { "Got Incremental start: $startI" }

    val outFsPath = try {
        if (tempFileSystem.isDirectory(outQualified)) {
            outQualified
        } else {
            null
        }
    } catch (_: Exception) {
        null
    } ?: PathUtil.getWithoutLastPathComponent(outQualified)


    val outputFileSystem = ScopedFileSystem(
        listOfNotNull(
            currentWorkingDirectory,
            outFsPath
        )
    )

    val getOutputName = getOutActual(
        fs = outputFileSystem,
        out = outQualified,
        startI = startI,
        increment = increment,
        isMultiPose = isMultiPose,
    )

    return Pair(outputFileSystem, getOutputName)
}


private fun getOutActual(
    fs: FileSystem,
    out: String,
    startI: Int,
    increment: Boolean,
    isMultiPose: Boolean,
): suspend (Int, String?) -> String {
    val previousFiles = mutableListOf<String>()
    return get@{ i: Int, outToTry: String? ->
        if (outToTry != null && !fs.fileExists(outToTry)) {
            return@get outToTry
        }
        var outActual = outToTry
        while (outActual == null || (fs.fileExists(outActual) && increment)) {
            Log.iIf(LOG_DEBUG) { "Outfile: $outActual already exists" }
            outActual = getOutputFileName(
                fs,
                outToTry ?: out,
                startI + i,
                isMultiPose = isMultiPose,
                increment = increment,
                previousFiles = previousFiles
            )
        }
        if (outActual !in previousFiles) {
            previousFiles.add(outActual)
        }
        outActual
    }
}
