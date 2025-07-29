package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.common.structs.BreedKey
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.app.exitNativeWithError
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.isNotNullOrEmpty
import com.bedalton.common.util.trySilent
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.eIf
import com.bedalton.log.iIf
import com.bedalton.vfs.LocalFileSystem


internal suspend fun SourceFiles.findGameVariant(): GameVariant {
    val files = getAllFiles()
    val isC1 = files["spr"].isNotNullOrEmpty()
    val isC2Maybe = files["s16"].isNotNullOrEmpty()
    val isC3Maybe = files["s16"].isNotNullOrEmpty()
    val isC2e = files["c16"].isNotNullOrEmpty()
    Log.iIf(LOG_DEBUG) { files.entries.joinToString("\n") { "${it.key} -> Count(${it.value.size})" } }
    val isC2 = if (isC2Maybe) {
        files["att"]?.any { path ->
            try {
                fs.readTextWindowsCP1252(path).split("\n").filter { it.isNotBlank() }.size == 10
            } catch (e: Exception) {
                Log.eIf(LOG_DEBUG) { "Failed to read file: $path; ${e.formatted()}" }
                false
            }
        } == true
    } else {
        false
    }
    val isC3: Boolean = if (!isC2e && isC3Maybe) {
        files["att"]?.any { path ->
            try {
                fs.readTextWindowsCP1252(path).split("\n").filter { it.isNotBlank() }.size == 16
            } catch (e: Exception) {
                Log.eIf(LOG_DEBUG) { "Failed to read C3 ATT file: $path; ${e.formatted()}" }
                false
            }
        } == true
    } else {
        isC2e
    }
    return when {
        (isC1 && !isC2 && !isC3) -> GameVariant.C1
        (isC2 && !isC1 && !isC3) -> GameVariant.C2
        (isC3 && !isC1 && !isC2) -> GameVariant.C3
        (!isC1 && !isC2 && !isC3) -> exitNativeWithError(1, "Failed to ascertain game variant type. Specify game variant with --game")
        else -> exitNativeWithError(1, "Too many game variants detected. Specify game variant with --game")
    }
}

private suspend fun SourceFiles.getAllFiles(recursive: Boolean = false): Map<String, List<String>> {
    Log.iIf(LOG_VERBOSE) { "Sources: $sources" }
    val files = sources.flatMap { source ->
        Log.iIf(LOG_VERBOSE) { "Checking source: $source" }
        LocalFileSystem!!.listAllFiles(source, recursive) { path ->
            val extension = PathUtil.getExtension(path)?.lowercase()
            extension in breedFileExtensions && BreedKey.isPartName(PathUtil.getFileNameWithoutExtension(path) ?: path)
        }
    }
    Log.iIf(LOG_DEBUG) { "Total Files: ${files.size}" }
    return breedFileExtensions.associateWith {  extension ->
        files.filter { path -> PathUtil.getExtension(path)?.lowercase() == extension }
    }
}

private val breedFileExtensions = setOf("spr", "s16", "c16", "att")