package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.agents.pray.data.DataBlock
import com.bedalton.creatures.agents.pray.parser.parsePrayAgentToMemory
import com.bedalton.common.coroutines.mapAsync
import com.bedalton.vfs.BinaryFile
import com.bedalton.vfs.File
import com.bedalton.vfs.FileSystem

internal suspend fun getAgentFiles(fs: FileSystem, allFiles: List<String>): List<Pair<String, List<File>>> {
    // Get agent files
    val agents = allFiles.filter { it.lowercase().endsWith("agent") || it.lowercase().endsWith("agents") }
    return agents.mapAsync map@{ path ->
        if (!fs.fileExists(path)) {
            return@map null
        }
        val bytes = fs.read(path)
        // GET FILE blocks
        val result = parsePrayAgentToMemory(bytes, whitelist = arrayOf("FILE"), true).blocks
            .filterIsInstance<DataBlock>()
            .filter { it.blockTag.lowercase() == "FILE" }

        // Create binary files from FILE blocks
        path to result.mapAsync {
                BinaryFile(
                    it.blockName,
                    it.data()
                )
            }
    }.filterNotNull()
}
