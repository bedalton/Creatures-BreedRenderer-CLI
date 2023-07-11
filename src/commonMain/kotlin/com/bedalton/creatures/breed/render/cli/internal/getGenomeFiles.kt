package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__INVALID_GENDER_VALUE
import com.bedalton.creatures.creature.common.c2.egg.EggFileParser
import com.bedalton.creatures.genetics.genome.Genome
import com.bedalton.creatures.genetics.parser.GenomeParser
import com.bedalton.app.exitNativeWithError
import com.bedalton.cli.unescapeCLIPathAndQualify
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.joinToString
import com.bedalton.common.util.pathSeparator
import com.bedalton.io.bytes.ByteStreamReader
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.log.wIf
import com.bedalton.vfs.ERROR_CODE__BAD_INPUT_FILE
import com.bedalton.vfs.File
import com.bedalton.vfs.expandTildeIfNecessary
import kotlin.random.Random

internal fun getGenomeFiles(
    genomePathIn: String?,
    currentWorkingDirectory: String?,
    agentFiles: List<Pair<String, List<File>>>
): Pair<List<String>, String?>? {
    if (genomePathIn == null) {
        return null
    }

    var genomePath = genomePathIn.expandTildeIfNecessary().stripDotSlash()
    val isRegexPath = isRegexPath(genomePath)
    if (!genomePath.lowercase().endsWith(".gen") && !genomePath.lowercase().endsWith(".egg")) {
        genomePath = if (isRegexPath) {
            "$genomePath\\.gen"
        } else {
            "$genomePath.gen"
        }
    }

    val absolutePath = if (!PathUtil.isAbsolute(genomePath) && agentFiles.isEmpty()) {
        if (currentWorkingDirectory == null) {
            throw Exception("Failed to get current working directory to qualify genome path")
        }
        unescapeCLIPathAndQualify(genomePath, currentWorkingDirectory)
            ?: throw Exception("Failed to qualify genome path; Genome File: $genomePath; Current Directory: $currentWorkingDirectory")
    } else {
        genomePath
    }

//    var agentGenomeName: String? = null

//    if (genomePath.contains(':')) {
//        val parts = genomePath.split(":", limit = 2)
//        genomePath = parts[0]
//        agentGenomeName = parts.getOrNull(1)?.let { "$it(\\.gen)?" }
//    } else if (isRegexPath) {
//        agentGenomeName = genomePath
//    }
//
//    if (PathUtil.getExtension(genomePath)?.lowercase()?.let { it == "agents" || it == "agent" } != true) {
//        return getGenomePathComponents(absolutePath)
//    }
//    return getGenomePathFromAgentsFile(genomePath, agentGenomeName, agentFiles)
    return Pair(listOf(absolutePath), null)
}


private fun getGenomePathComponents(absolutePath: String): Pair<List<String>, String?> {
    if (!isRegexPath(absolutePath)) {
        return Pair(listOf(absolutePath), null)
    }
    val pathSeparator = if (absolutePath.contains('/')) '/' else '\\'
    val components = if (pathSeparator == '\\') {
        splitKeepingEscapes(absolutePath, '\\')
    } else {
        absolutePath.split('/')
    }
    val parentPath = components.takeWhile { !isRegexPath(it.replace("\\\\.".toRegex(), "")) }.joinToString(pathSeparator)
    return Pair(listOf(parentPath), absolutePath)
}
//
//private fun getGenomePathFromAgentsFile(genomePath: String, agentGenomeName: String?, agentFiles: List<Pair<String, List<File>>>): Pair<List<String>, String?>? {
//
//    Log.iIf(LOG_DEBUG) { "Genome file passed in was an PRAY agents file: <$genomePath>" }
//
//    val regex = if (agentGenomeName != null) ".*?$agentGenomeName".toRegex(RegexOption.IGNORE_CASE) else null
//
//    val fileName = PathUtil.getLastPathComponent(genomePath)?.lowercase()
//
//
//
//    // Find first agent matching the requested name
//    val theAgent: List<Pair<String, List<File>>> = agentFiles
//        .filter { it.first like genomePath }.nullIfEmpty()
//        ?: agentFiles.filter {
//            PathUtil.getLastPathComponent(it.first)?.lowercase() == fileName
//        }.nullIfEmpty()
//        ?: if (agentGenomeName == null) {
//            agentFiles
//        } else {
//            return null
//        }
//
//
//    // Get genome name
//    var genomeFiles: List<Pair<String, File>> = theAgent.flatMap { agentWithFiles ->
//        agentWithFiles.second.mapNotNull {
//            if (PathUtil.getExtension(it.name) like "gen") {
//                Pair(agentWithFiles.first, it)
//            } else {
//                null
//            }
//        }
//    }
//
//    if (genomeFiles.size > 1 && regex != null) {
//        val temp = genomeFiles.filter { regex.matches(it.second.name) }
//        if (temp.isNotEmpty()) {
//            genomeFiles = temp
//        } else {
//            return null
//        }
//    }
//
//    // Convert paths to agent relative paths
//    val fileNames = genomeFiles
//        .map { PathUtil.combine(it.first, it.second.name) }
//        .nullIfEmpty()
//        ?: return null
//    return Pair(fileNames, null)
//}


/**
 * Reads genome from path
 * If C2 EGG, egg gender is also returned
 * @return Pair(first = Genome, second = EggGender)
 */
internal suspend fun readGenome(
    sourceFiles: SourceFiles,
    genomePath: String,
    genomeVariant: Int? = null
): Pair<Genome, Int?> {

    // Get bytes for file
    val genomeBytes = try {
        sourceFiles.fs.read(genomePath)
    } catch (e: Exception) {
        exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE, "Failed to get genome bytes; ${e.formatted()}")
    }

    // if Egg, read egg and return
    if (PathUtil.getExtension(genomePath)?.lowercase() == "egg") {
        Log.iIf(LOG_DEBUG) { "Reading genome from Egg at $genomePath" }
        return readGenomeFromEgg(genomeBytes, genomeVariant)
    }

    return try {

        Log.iIf(LOG_DEBUG) { "Reading genome from $genomePath" }
        Pair(GenomeParser.parseGenome(genomeBytes), null)
    } catch (e: Exception) {
        exitNativeWithError(1, "Failed to parse genome: " + e.message)
    }
}


private suspend fun readGenomeFromEgg(genomeBytes: ByteArray, genomeVariant: Int? = null): Pair<Genome, Int> {
    val genomeIndex = if (genomeVariant == null || genomeVariant !in 1..2) {
        if (genomeVariant != null && genomeVariant != 0) {
            Log.e { "Invalid egg genome index. Using random value" }
        }
        Random.nextInt(0, 1)
    } else {
        genomeVariant - 1
    }
    val egg = try {
        ByteStreamReader.read(genomeBytes) { EggFileParser.parse(this) }
    } catch (e: Exception) {
        exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE, "Failed to parse C2 egg file; ${e.formatted()}")
    }

    val gender = egg.gender
    val eggGenome = egg[genomeIndex] ?: egg.mum

    return try {
        Pair(eggGenome.genome(), gender)
    } catch (e: Exception) {
        exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE, "Failed to parse egg genome; ${e.formatted()}")
    }
}

internal fun genderFromC2EggGenderValue(eggGenderValue: Int, fallbackGender: Int?): Int {
    if (eggGenderValue !in 0..3) {
        if (fallbackGender == null) {
            exitNativeWithError(RENDER_ERROR_CODE__INVALID_GENDER_VALUE, "C2 egg gender was invalid")
        }
        val genderString = getEggGenderValueGender(fallbackGender) ?: "?"
        Log.wIf(LOG_DEBUG) { "C2 egg gender <$eggGenderValue> was invalid; but gender was set to <$genderString> from command line" }
        return fallbackGender
    }


    val genderValueGender = getEggGenderValueGender(eggGenderValue)

    // If gender is 0 or 2 (Norn Binary), use random gender
    val outputGender = if (eggGenderValue != 1 && eggGenderValue != 2) {
        val randomGender = Random.nextInt(0, 1) + 1
        val resolvedGenderString = getEggGenderValueGender(randomGender + 1)
        Log.iIf(LOG_DEBUG) { "Resolved C2 egg <$genderValueGender> gender to $resolvedGenderString" }
        randomGender
    } else {
        Log.iIf(LOG_DEBUG) { "Using gender <${getEggGenderValueGender(eggGenderValue)}> from C2 egg" }
        eggGenderValue
    }
    return outputGender
}

internal fun isRegexPath(path: String): Boolean {
    val regexChars = "\\.?[*?+|]".toRegex()
    return regexChars.containsMatchIn(path)
}

private fun splitKeepingEscapes(path: String, separator: Char): List<String> {
    val escapableChars = listOf('n', 't', '?', '$', '^', '*', '+', 'r', '!')
    val hasEscapedRegexSymbols = escapableChars.joinToString("\\\\").toRegex()
    if (separator == '/' || !hasEscapedRegexSymbols.matches(path)) {
        return path.split(separator)
    }
    val replacer = ("\\(" + escapableChars.joinToString("|").toRegex() + ")").toRegex()
    var escape = "@@:_SLASH__:@@"
    while (path.contains(escape)) {
        escape = ":_${escape}_:"
    }
    val pathConverted = path.replace(replacer, "$escape$1")
    return pathConverted
        .split(pathSeparator)
        .map { it.replace(escape, "\\") }
}