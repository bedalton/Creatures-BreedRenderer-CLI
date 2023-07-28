package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.coroutines.mapAsync
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__FAILED_TO_RENDER
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__FAILED_TO_WRITE_IMAGE
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__MISSING_POSE
import com.bedalton.creatures.breed.render.support.pose.Pose
import com.bedalton.log.*
import com.bedalton.vfs.FileSystem
import com.bedalton.vfs.openFile
import korlibs.image.bitmap.Bitmap32
import korlibs.image.format.PNG
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


private val fileNameLock = Mutex(false)

internal suspend fun renderPoses(
    fs: FileSystem,
    open: Boolean,
    poses: List<Pair<Pose,String?>>,
    getOutputName: suspend(Int, String?) -> String,
    renderer: suspend (pose: Pose, zoom: Double?) -> Bitmap32?,
): Boolean {
    // Make sure there are poses
    if (poses.isEmpty()) {
        exitWithError(RENDER_ERROR_CODE__MISSING_POSE)
    }

    // Log poses if asked
    Log.iIf(LOG_DEBUG) { "Pose list: $poses" }

    val poseData = poses.indices.map { i ->
        formatRenderData(
            i = i,
            poses = poses,
            getOutActual = getOutputName
        )
    }
    // Render pose(s)
    val rendered = poseData.mapAsync { (i, outToTry, pose) ->
        render(
            fs,
            renderer,
            i,
            outToTry,
            pose.first,
            open,
            getOutputName
        )
    }
    return rendered.all{ it }
}

private suspend fun formatRenderData(
    poses: List<Pair<Pose, String?>>,
    i: Int,
    getOutActual: suspend (Int, String?) -> String,
): Triple<Int, String, Pair<Pose, String?>> {
    val pose = poses[i]

    Log.iIf(LOG_VERBOSE) { "Getting out file name" }

    // Get actual file name to write to.
    // This could have been set on the CLI or could be automatically incremented
    val outActual = pose.second // get explicitly set name
        ?.replace("[\\\\/:]".toRegex(), "_")  // Replace illegal chars
        ?: getOutActual(i, null)
    Log.iIf(LOG_VERBOSE) { "Got out file name: $outActual" }

    return Triple(i, outActual, pose)
}

private suspend fun render(
    fs: FileSystem,
    renderer: suspend (Pose, Double?) -> Bitmap32?,
    i: Int,
    outToTry: String,
    pose: Pose,
    open: Boolean,
    getOutActual: suspend (Int, String?) -> String
): Boolean {

    // Actually render the image
    val rendered = renderer(pose, null)
        ?: exitWithError(RENDER_ERROR_CODE__FAILED_TO_RENDER)

    // Convert Bitmap32 to PNG bytes
    val outputBytes = PNG.encode(rendered)

    return try {
        fileNameLock.withLock {
            val outActual = getOutActual(i, outToTry)
            // Log actual filename
            Log.iIf(LOG_DEBUG) { "writing: $outActual" }
            fs.write(outActual, outputBytes)

            Log.iIf(LOG_DEBUG) { "Wrote Pose: $i to $outActual" }
            if (open) {
                if (!openFile(fs, outActual, Log.hasMode(LOG_DEBUG))) {
                    Log.eIf(LOG_DEBUG) { "Failed to open $outActual in default program" }
                } else {
                    Log.iIf(LOG_DEBUG) { "Should have opened file: ${outActual} in default program" }
                }
            }
            true
        }
    } catch (e: Exception) {
        exitWithError(RENDER_ERROR_CODE__FAILED_TO_WRITE_IMAGE, outToTry, stackTrace = e.stackTraceToString())
    }
}
