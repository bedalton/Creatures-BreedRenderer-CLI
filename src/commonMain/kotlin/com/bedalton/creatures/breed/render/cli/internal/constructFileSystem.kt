package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.app.exitNativeWithError
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.isNotNullOrBlank
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.vfs.*


internal suspend fun constructFileSystem(
    rawSourcesList: List<String>,
    currentWorkingDirectory: String?,
    genomePathIn: String?,
    exportPath: String?
): SourceFiles {
    Log.iIf(LOG_DEBUG) { "Command running with ${rawSourcesList.size} source paths:\n\t-${rawSourcesList.joinToString("\n\t-")}" }


    @Suppress("RegExpRedundantEscape")
    val aliasPathPattern = "\\[([^\\]]+)\\]=(.+)".toRegex()

    val sourceRootDirectories = (rawSourcesList.map {
        val path = aliasPathPattern.matchEntire(it)
            ?.groupValues
            ?.getOrNull(2)
            ?: it
        if (PathUtil.getExtension(path).isNotNullOrBlank()) {
            (PathUtil.getWithoutLastPathComponent(path) ?: path).unescapePath()
        } else {
            path.unescapePath()
        }

    }).distinct()


    Log.iIf(LOG_DEBUG) {
        "SourceDirectories:\n\t- " + sourceRootDirectories.joinToString("\n\t- ")
    }

    // Get file system with actual paths
    val fsPhysical = UnscopedFileSystem()

    val missing = sourceRootDirectories.filterNot { fsPhysical.fileExists(it) }

    if (missing.isNotEmpty()) {
        exitNativeWithError(ERROR_CODE__BAD_INPUT_FILE) {
            "Invalid source files:\n\t-${missing.joinToString("\n\t-")}"
        }
    }

    // Get files with aliased paths where the file does not match the filename in the FileSystem
    val aliasedPaths = rawSourcesList.mapNotNull map@{
        val parts = aliasPathPattern
            .matchEntire(it)
            ?.groupValues
            ?.drop(1)
            ?: return@map null
        var alias = parts[0]
        if (!alias.startsWith('/')) {
            alias = "/$alias"
        }
        AliasedFile(
            fsPhysical,
            alias,
            parts[1],
            null
        )
    }

    // Combine physical and aliased source files
    val sourcesWithAliasedNames = rawSourcesList.filterNot(aliasPathPattern::matches) + aliasedPaths.map { it.path }

    Log.iIf(LOG_DEBUG) {
        "${sourcesWithAliasedNames.size} sources with aliased names:\n\t- ${
            sourcesWithAliasedNames.joinToString(
                "\n\t- "
            )
        }"
    }


    // Construct file system
    val fs = FileSystemWithInputFiles(
        fsPhysical,
        aliasedPaths.associateBy { it.path },
        matchFileNameOnly = true
    )


    // Get expected genome files
    val genomeFiles = getGenomeFiles(genomePathIn, currentWorkingDirectory, emptyList())

    val exportPathQualified = if (exportPath != null) {
        if (!PathUtil.isAbsolute(exportPath)) {
            if (currentWorkingDirectory == null) {
                throw Exception("Cannot get export without fully qualified path")
            } else {
                PathUtil.combine(currentWorkingDirectory, exportPath)
            }
        } else {
            exportPath
        }
    } else {
        null
    }

    if (exportPathQualified != null) {
        Log.iIf(LOG_DEBUG) { "ExportPathIn: $exportPath; Qualified: $exportPathQualified" }
    } else {
        Log.iIf(LOG_VERBOSE) { "No export path passed in" }
    }

    return SourceFiles(
        fs,
        sources = sourcesWithAliasedNames,
        genomeFiles = genomeFiles?.first,
        genomeFilter = genomeFiles?.second,
        exportPath = exportPathQualified
    )
}
