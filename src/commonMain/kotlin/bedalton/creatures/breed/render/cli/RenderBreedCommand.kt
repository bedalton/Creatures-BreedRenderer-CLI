package bedalton.creatures.breed.render.cli

import bedalton.creatures.breed.render.cli.internal.*
import bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import bedalton.creatures.breed.render.support.pose.Pose.Companion.defaultPose
import bedalton.creatures.cli.GameArgType
import bedalton.creatures.common.structs.BreedKey
import bedalton.creatures.common.structs.GameVariant
import com.bedalton.app.exitNativeWithError
import com.bedalton.app.getCurrentWorkingDirectory
import com.bedalton.cli.Flag
import com.bedalton.common.util.*
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.vfs.*
import korlibs.image.format.PNG
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlin.random.Random


class RenderBreedCommand : Subcommand("render", "Render a creature with breed options") {

    private val gameVariant by argument(
        GameArgType,
        "game",
        "Game to render for"
    )

    private val debug by option(
        Flag,
        "debug",
        "Add additional debug logging"
    )

    private val out by argument(
        ArgType.String,
        "output",
        "Output PNG file"
    )

    private val head by option(
        PartArg,
        "head",
        "h",
        "The breed to use for the head (including ears and hair (if no other hair is set))"
    )

    private val hair by option(
        PartArg,
        "hair",
        description = "Breed to use for the hair"
    )

    private val body by option(
        PartArg,
        "body",
        "b",
        description = "Breed to use for the body"
    )

    private val arms by option(
        PartArg,
        "arms",
        "a",
        "The breed to use for the arms"
    )

    private val legs by option(
        PartArg,
        "legs",
        "l",
        "The breed to use for the legs"
    )

    private val tail by option(
        PartArg,
        "tail",
        "t",
        "The breed to use for the tail"
    )

    private val poses by option(
        ArgType.String,
        "pose",
        "p",
        "The creature pose to render"
    ).multiple()

    private val breed by option(
        PartArg,
        "breed",
        "The breed to render (default for all missing parts)"
    )

    private val age by option(
        ArgType.Int,
        "age",
        description = "Creature age as int 0..7"
    ).required()

    private val gender by option(
        GenderArg,
        "gender",
        "g",
        "Creature gender"
    )

    private val scale: Double by option(
        ArgType.Double,
        "scale",
        "s",
        "The amount to scale the rendered image by, Scale can be 1..10 (inclusive)"
    ).default(1.0)

    private val transformVariant: GameVariant? by option(
        GameArgType,
        "transform-variant",
        description = "The variant function to use for tint, swap and rotate"
    )

    private val tint by option(
        ArgType.String,
        "tint",
        description = "Tint in the format {r}:{g}:{b} where value is 0-255. Example: 10:255:13",
    )

    private val swap by option(
        ArgType.Int,
        "swap",
        description = "Color swap between red and blue. 0-255, with 128 being no change"
    )

    private val rotation by option(
        ArgType.Int,
        "rotation",
        description = "Color rotation shifting. 0-255, with 128 being no change"//. Value less than 128 rotated r<-g<-b. Greater than 128 r->g->b"
    )

    private val exactMatch by option(
        Flag,
        "exact-match",
        "x",
        "Match sprite files exactly (no substituting age or gender) or fail"
    ).default(false)

    private val sources by argument(
        ArgType.String,
        "sources",
        "Source folders and files"
    ).vararg()

    @Suppress("SpellCheckingInspection")
    private val ghost by option(
        PartsArg,
        "ghost",
        description = "Parts to render as semi-transparent; Usage --ghost a,b,c,d or --ghost abcd"
    ).multiple()

    private val ghostAlpha by option(
        ArgType.Double,
        "ghost-alpha",
        description = "The alpha to use for ghost parts. 0.0..1.0 with 0 being completely transparent"
    )

    private val renderGhostPartsBelow by option(
        Flag,
        "ghost-parts-below",
        description = "Render ghost parts below solid ones"
    ).default(false)

    @Suppress("SpellCheckingInspection")
    private val hidden by option(
        PartsArg,
        "hidden",
        description = "List of parts not to render. Usage --hidden a,b,c,d or --hidden abcd",
    ).multiple()

    private val trim by option(
        Flag,
        "trim",
        description = "Trim surrounding whitespace around fully rendered image before adding padding"
    ).default(false)

    private val padding by option(
        ArgType.Int,
        "padding",
        "d",
        "Image padding around image. When not used with --trim, surrounding whitespace can be more than expected"
    )


    private val mood by option(
        MoodArg,
        "mood",
        "m",
        "The mood to render the creature in"
    )

    private val increment by option(
        Flag,
        "increment",
        "i",
        "Increment file numbers"
    ).default(false)


    private val eyesClosed by option(
        Flag,
        "closed",
        "c",
        description = "Eyes closed"
    )


    private val nonIntersectingLimbs by option(
        Flag,
        "no-intersect",
        description = "Use non-C2e limb z-order to prevent limb parts rendering both above and below another"
    ).default(false)


    private val genomePath by option(
        ArgType.String,
        "genome",
        description = "Genetics file or C2 egg file"
    )

    private val genomeVariant by option(
        ArgType.Int,
        "gene-variant",
        description = "C2e genome variant, used when determining parts and "
    ).default(0)

    private val verbose by option(
        Flag,
        "verbose",
        description = "Use verbose logging"
    ).default(false)

    override fun execute() {
        GlobalScope.launch {
            executeSuspending()
        }
    }

    suspend fun executeWithResult(): Int {
        return withContext(Dispatchers.Default) {
            executeSuspending()
        }
    }

    private suspend fun executeSuspending(): Int {

        if (debug == true) {
            Log.setMode(LOG_DEBUG, true)
        }
        if (verbose) {
            Log.setMode(LOG_VERBOSE, true)
            Log.setMode(LOG_DEBUG, true)
        }

        if (scale !in 1.0..10.0) {
            exitNativeWithError(1, "Scale must be be 1..10 (inclusive); Found: $scale")
        }


        val missing = mutableListOf<String>()

        // Age
        val age = age
        if (age !in 0..6) {
            exitWithError(RENDER_ERROR_CODE__INVALID_AGE_VALUE)
        }

        // Age or gender is missing
        if (missing.isNotEmpty()) {
            exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER, missing.joinToString(""))
        }


        // Get working directory
        val currentWorkingDirectory = getCurrentWorkingDirectory()?.expandTildeIfNecessary()?.stripDotSlash()

        // Combine the sources which are both globbed files, and folders
        val sources = getSourceFolders(currentWorkingDirectory)

        val finalSources = constructFileSystem(sources, currentWorkingDirectory, genomePath)

        // Create initial builder
        var task = BreedRendererBuilder(gameVariant)
            .withSetSourceRoots(finalSources.sources + listOfNotNull(finalSources.getGenomePath()))
            .withAge(age)
            .withGhostParts(ghost.flatten())
            .withHiddenParts(hidden.joinToString { "$it" })
            .withGhostAlpha(ghostAlpha)
            .withExactMatch(exactMatch)
            .withRenderGhostPartsBelow(renderGhostPartsBelow)
            .withTrimWhitespace(trim)
            .withNonIntersectingLimbs(nonIntersectingLimbs)
            .withPadding(padding)
            .applySwapAndRotation()

        val genomePath = finalSources.getGenomePath()
        val genomeVariant = if (genomeVariant !in 0..8) {
            Log.e { "Genome variant must be a value 0..8; Found: $genomeVariant; Defaulting to '0'" }
            0
        } else {
            genomeVariant
        }

        // Gender // Possibly
        var gender = gender


        if (gender == -1) {
            gender = (Random.nextInt(0, 99) % 2) + 1
            Log.iIf(LOG_DEBUG) { "Resolving random gender to ${getEggGenderValueGender(gender!!)} " }
        }

        // Collect missing breed information
        if (genomePath.isNotNullOrBlank()) {
            val (genome, genderIfC2Egg) = readGenome(finalSources, genomePath, genomeVariant)
            // Apply gender from C2 egg
            if (genderIfC2Egg != null) {
                gender = genderFromC2EggGenderValue(genderIfC2Egg, gender)
            }
            if (gender == null) {
                exitNativeWithError(RENDER_ERROR_CODE__INVALID_GENDER_VALUE, "Gender is required")
            }

            task = task.withGenomeApplyNow(genome, gender, age, if (genderIfC2Egg != null) 0 else genomeVariant)

            task = task.withAge(age)
            task = task.withFallbackGender(gender)
            Log.iIf(LOG_DEBUG) { "CLI: " + task.toCLIOpts(false) }


            Log.iIf(LOG_DEBUG) { "Applied parts from genome" }
            head?.let { task = task.withHead(it) }
            body?.let { task = task.withBody(it) }
            legs?.let { task = task.withLegs(it) }
            arms?.let { task = task.withArms(it) }
            tail?.let { task = task.withTail(it) }
            hair?.let { task = task.withHair(it) }

        } else {
            collectMissingBreedParts(missing)

            // Breeds are missing
            if (missing.size > 0) {
                exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES, missing.joinToString(","))
            }
            task = task.applyBreeds()
        }

        // Make sure gender is not null (not null if passed from CLI or set by C2 Egg)
        if (gender == null) {
            exitNativeWithError(RENDER_ERROR_CODE__INVALID_GENDER_VALUE, "Value for option --gender must be provided")
        }

        // Set gender
        task = task.withFallbackGender(gender)


        // Get out base path. Could be a file or a folder
        val outWithExpandedTilde = out.expandTildeIfNecessary().stripDotSlash()
        // Get path while expanding tilde, as this failed on Ubuntu
        val out = if (!PathUtil.isAbsolute(outWithExpandedTilde)) {
            if (currentWorkingDirectory == null) {
                throw Exception("Failed to get current working directory")
            }
            PathUtil.combine(currentWorkingDirectory, outWithExpandedTilde)
        } else {
            outWithExpandedTilde
        }

        // Get the actual pose renderer
        val renderer = task.poseRenderer()

        // Create RegEx for finding if a pose has an associated file name
        val poseWithNameRegex = "([0-5Xx!?]{15})[:,\\-=](.*)".toRegex()

        // Map pose strings to poses
        val poses = (poses.ifEmpty { listOf(defaultPose(gameVariant).toPoseString()) })
            .map { poseIn ->
                val temp = poseWithNameRegex
                    .matchEntire(poseIn.stripSurroundingQuotes())
                    ?.groupValues
                    ?.drop(0)
                val pose = temp?.getOrNull(0) ?: poseIn
                val fileName = temp?.getOrNull(1)
                Pair(resolvePose(gameVariant, pose, mood, eyesClosed), fileName)
            }

        // Make sure there are poses
        if (poses.isEmpty()) {
            exitWithError(RENDER_ERROR_CODE__MISSING_POSE)
        }

        // Log poses if asked
        Log.iIf(LOG_DEBUG) { "Pose list: $poses" }

        val outFsPath = if (LocalFileSystem?.isDirectory(out) == true) {
            out
        } else {
            PathUtil.getWithoutLastPathComponent(out)
        }

        val fs = ScopedFileSystem(
            listOfNotNull(
                currentWorkingDirectory,
                outFsPath
            )
        )

        // Get start file number increment, for sequential files
        val startI = getIncrementalFileStart(fs, out, poses.count { it.second == null })

        val lastIndex = poses.lastIndex
        // Render pose(s)
        val wroteAll = poses.indices.map { i ->
            val pose = poses[i]

            // Get actual file name to write to.
            // This could have been set on the CLI or could be automatically incremented
            val outActual = pose.second // get explicitly set name
                ?.replace("[\\\\/:]".toRegex(), "_")  // Replace illegal chars
                ?: getOutputFileName(fs, out, startI + i, isMultiPose = poses.size <= 1, increment = increment)

            // Log actual filename
            Log.iIf(LOG_DEBUG) { "writing: $outActual" }

            // Actually render the image
            val rendered = renderer(pose.first, scale)
                ?: exitWithError(RENDER_ERROR_CODE__FAILED_TO_RENDER)

            // Convert Bitmap32 to PNG bytes
            val outputBytes = PNG.encode(rendered)

            try {
                fs.write(outActual, outputBytes)
                if (i != lastIndex) {
                    delay(20)
                }
                Log.iIf(LOG_DEBUG) { "Wrote Pose: $i to $outActual" }
                true
            } catch (e: Exception) {
                exitWithError(RENDER_ERROR_CODE__FAILED_TO_WRITE_IMAGE, out, stackTrace = e.stackTraceToString())
            }
        }.all { it }
        // Write PNG bytes to output file
        return if (wroteAll) 0 else 1
    }

    private fun BreedRendererBuilder.applySwapAndRotation(): BreedRendererBuilder {
        // Modifiable task
        var out = this

        // Validate tint values
        val rgb = getTintValues(tint)

        // Store for non-null checks and use
        var swap = swap
        if (swap != null && swap < 0) {
            swap = Random.nextInt(0, 256)
        }
        var rotation = rotation
        if (rotation != null && rotation < 0) {
            rotation = Random.nextInt(0, 256)
        }

        // Apply transform variant if any
        val transformVariant = transformVariant
        if (transformVariant != null) {
            out = out.withPaletteTransformVariant(transformVariant)
        }

        // Apply swap and rotate if needed
        return if (rgb != null || swap != null || rotation != null) {
            out.withTintSwapAndRotate(
                red = rgb?.get(0),
                green = rgb?.get(1),
                blue = rgb?.get(2),
                swap = swap,
                rotation = rotation,
                throwOnInvalidValue = true
            )
        } else {
            out
        }
    }

    private fun toKey(breedKey: BreedKey): BreedKey {
        if (breedKey.genus !in 0..3) {
            exitNativeWithError(RENDER_ERROR_CODE__INVALID_GENUS, "Invalid part breed genus: ${breedKey.genus}")
        }
        val breed = breedKey.breed?.lowercaseChar()
            ?: exitNativeWithError(
                RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES,
                "Part breed is required"
            )
        if (gameVariant == GameVariant.C1) {
            if (breed !in '0'..'9') {
                exitNativeWithError(
                    RENDER_ERROR_CODE__INVALID_BREED,
                    "Invalid part breed: ${breedKey.breed}; Expected 0..9"
                )
            }
        } else if (breed !in 'a'..'z') {
            exitNativeWithError(
                RENDER_ERROR_CODE__INVALID_BREED,
                "Invalid part breed: ${breedKey.breed}; Expected a..z"
            )
        }
        var out = breedKey
        if (out.gender == null) {
            out = out.copyWithGender(gender)
        }
        return out.copyWithAgeGroup(age)
    }

    private fun collectMissingBreedParts(missing: MutableList<String>) {
        val default = breed
        val head = head ?: default
        val body = body ?: default
        val legs = legs ?: default
        val arms = arms ?: default
        val tail = tail ?: default

        if (head == null) {
            missing.add("head")
        }
        if (body == null) {
            missing.add("body")
        }
        if (legs == null) {
            missing.add("legs")
        }
        if (arms == null) {
            missing.add("arms")
        }
        if (tail == null && gameVariant != GameVariant.C1) {
            missing.add("tail")
        }
    }

    private fun assertNotMissing(missing: List<String>, genomeVariant: Int? = null) {
        // Breeds are missing
        if (missing.isNotEmpty()) {
            val variantText = if (genomeVariant != null && genomeVariant != 0) {
                " in genome with variant $genomeVariant"
            } else {
                ""
            }
            exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES, missing.joinToString(",") + variantText)
        }
    }

    private fun BreedRendererBuilder.applyBreeds(): BreedRendererBuilder {
        val default = breed
        val head = head ?: default
        val hair = hair ?: default
        val body = body ?: default
        val legs = legs ?: default
        val arms = arms ?: default
        val tail = tail ?: default

        var out = withHead(toKey(head!!))
            .withBody(toKey(body!!))
            .withArms(toKey(arms!!))
            .withLegs(toKey(legs!!))
        if (tail != null) {
            out = out.withTail(toKey(tail))
        }
        if (hair != null) {
            out = out.withHair(toKey(hair))
        }
        return out
    }


    /**
     * Get source folders from CLI source arguments
     */
    private suspend fun getSourceFolders(currentWorkingDirectory: String?): List<String> {

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
}


private suspend fun constructFileSystem(
    rawSourcesList: List<String>,
    currentWorkingDirectory: String?,
    genomePathIn: String?
): SourceFiles {
    Log.iIf(LOG_DEBUG) { "Command running with ${rawSourcesList.size} source paths;" }


    val aliasPathPattern = "\\[([^\\]]+)\\]=(.+)".toRegex()

    val sourceRootDirectories = rawSourcesList.map {
        val path = aliasPathPattern.matchEntire(it)
            ?.groupValues
            ?.getOrNull(2)
            ?: it
        (PathUtil.getWithoutLastPathComponent(path) ?: path).unescapePath()
    }.distinct()

    Log.iIf(LOG_DEBUG) {
        "SourceDirectories:\n\t- " + sourceRootDirectories.joinToString("\n\t- ")
    }

    // Get file system with actual paths
    val fsPhysical = ScopedFileSystem(sourceRootDirectories)

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

    // Construct file system
    val fs = FileSystemWithInputFiles(
        fsPhysical,
        aliasedPaths.associateBy { it.path },
        matchFileNameOnly = true
    )

//    // Get agent files
//    val agentFiles = getAgentFiles(fs, sourcesWithAliasedNames)
//
//    for ((agentFileName, files) in agentFiles) {
//        for (file in files) {
//            fs[PathUtil.combine(agentFileName, file.name)] = file
//        }
//    }

    // Get expected genome files
    val genomeFiles = getGenomeFiles(genomePathIn, currentWorkingDirectory, emptyList())

    return SourceFiles(
        fs,
        sources = sourcesWithAliasedNames,
        genomeFiles = genomeFiles?.first,
        genomeFilter = genomeFiles?.second
    )
}

