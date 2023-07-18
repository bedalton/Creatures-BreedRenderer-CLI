package com.bedalton.creatures.breed.render.cli

import com.bedalton.app.exitNativeWithError
import com.bedalton.app.getCurrentWorkingDirectory
import com.bedalton.cli.Flag
import com.bedalton.cli.promptYesNo
import com.bedalton.common.coroutines.mapAsync
import com.bedalton.common.util.*
import com.bedalton.creatures.breed.render.cli.internal.*
import com.bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import com.bedalton.creatures.breed.render.renderer.getColorTransform
import com.bedalton.creatures.breed.render.support.pose.Pose.Companion.defaultPose
import com.bedalton.creatures.cli.GameArgType
import com.bedalton.creatures.common.structs.BreedKey
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.creatures.common.util.getGenusString
import com.bedalton.creatures.genetics.genome.Genome
import com.bedalton.creatures.sprite.util.PaletteTransform
import com.bedalton.log.*
import com.bedalton.log.ConsoleColors.BLACK_BACKGROUND
import com.bedalton.log.ConsoleColors.BOLD
import com.bedalton.log.ConsoleColors.RESET
import com.bedalton.log.ConsoleColors.UNDERLINE_WHITE
import com.bedalton.log.ConsoleColors.WHITE
import com.bedalton.vfs.*
import korlibs.image.format.PNG
import kotlinx.cli.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random


class RenderBreedCommand(
    val args: Array<String>
) : Subcommand(
    "render-creatures",
    "Render a creature with breed options"
) {

    private val gameVariant by option(
        GameArgType,
        "game",
        "Game to render for. Value of [C1,C2,C3]"
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
    )

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
        ArgType.String,
        "swap",
        description = "Color swap between red and blue. 0-255, with 128 being no change"
    )

    private val rotation by option(
        ArgType.String,
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

    private val exportPath by option(
        ArgType.String,
        "export",
        description = "Creature export file path"
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

    private val fileNameLock = Mutex(false)

    private val open by option(
        Flag,
        "open",
        description = "Attempt to open rendered image in default image program"
    ).default(false)

    private val usesRandom by lazy {
        val colorValues = ((tint ?: "") + (swap ?: "") + (rotation ?: ""))
        colorValues.contains('*') || colorValues.contains("rand")
    }

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


        // Age or gender is missing
        if (missing.isNotEmpty()) {
            exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER, missing.joinToString(""))
        }


        // Get working directory
        val currentWorkingDirectory = getCurrentWorkingDirectory()?.expandTildeIfNecessary()?.stripDotSlash()

        // Combine the sources which are both globbed files, and folders
        val sources = getSourceFolders(currentWorkingDirectory)

        if (genomePath != null && exportPath != null) {
            exitNativeWithError(1, "Genome and Export options cannot be used together")
        }
        val finalSources = constructFileSystem(sources, currentWorkingDirectory, genomePath, exportPath)

        val gameVariant = gameVariant
            ?: finalSources.findGameVariant().also {
                Log.iIf(LOG_DEBUG) { "Ascertained variant to be $it" }
            }


        // Create initial builder
        var task = BreedRendererBuilder(gameVariant)
            .withSetSourceRoots(finalSources.sources + listOfNotNull(finalSources.getGenomePath()))
            .withGhostParts(ghost.flatten())
            .withHiddenParts(hidden.joinToString { "$it" })
            .withGhostAlpha(ghostAlpha)
            .withExactMatch(exactMatch)
            .withRenderGhostPartsBelow(renderGhostPartsBelow)
            .withTrimWhitespace(trim)
            .withNonIntersectingLimbs(nonIntersectingLimbs)
            .withPadding(padding)

        val genomePath = finalSources.getGenomePath()
        val genomeVariant = if (genomeVariant !in 0..8) {
            Log.e { "Genome variant must be a value 0..8; Found: $genomeVariant; Defaulting to '0'" }
            0
        } else {
            genomeVariant
        }

        // Age
        var age = age

        // Gender
        var gender = gender

        if (gender == -1) {
            gender = (Random.nextInt(0, 99) % 2) + 1
            Log.iIf(LOG_DEBUG) { "Resolving random gender to ${getEggGenderValueGender(gender!!)} " }
        }

        var setParts = false
        var genome: Genome? = null

        // Collect missing breed information
        if (genomePath.isNotNullOrBlank()) {
            val (genomeTemp, genderIfC2Egg) = readGenome(finalSources, genomePath, genomeVariant)
            genome = genomeTemp
            // Apply gender from C2 egg
            if (genderIfC2Egg != null) {
                gender = genderFromC2EggGenderValue(genderIfC2Egg, gender)
            }
        }

        // Get data from imports
        val exportData = finalSources.exportData.getData()
        if (exportData != null) {
            Log.iIf(LOG_DEBUG) { "Export Data:\n$exportData" }
            if (exportData.size > 1) {
                Log.w { "Too many exports found in file. Using first" }
            }
            val export = exportData.first()
            genome = export.genome
            gender = gender ?: export.gender
            age = age ?: export.age
        } else {
            Log.iIf(LOG_VERBOSE) { "No export data" }
        }

        if (age == null) {
            exitNativeWithError(RENDER_ERROR_CODE__INVALID_AGE_VALUE, "Age value is required; Use \'--age\'")
        }

        if (age !in 0..6) {
            exitWithError(RENDER_ERROR_CODE__INVALID_AGE_VALUE)
        }

        task = task.withAge(age)

        // Make sure gender is not null (not null if passed from CLI or set by C2 Egg)
        if (gender == null) {
            exitNativeWithError(RENDER_ERROR_CODE__INVALID_GENDER_VALUE, "Value for option --gender must be provided")
        }



        if (genome != null) {
            task = task.withGenomeApplyNow(genome, gender, age, if (genome.version < 3) 0 else genomeVariant)

            task = task.withAge(age)
            task = task.withFallbackGender(gender)


            Log.iIf(LOG_DEBUG) { "Applied parts from genome" }
            setParts = true

            (head ?: breed)?.let { task = task.withHead(it) }
            (body ?: breed)?.let { task = task.withBody(it) }
            (legs ?: breed)?.let { task = task.withLegs(it) }
            (arms ?: breed)?.let { task = task.withArms(it) }
            (tail ?: breed)?.let { task = task.withTail(it) }
            (hair ?: breed)?.let { task = task.withHair(it) }
        }



        if (!setParts) {
            collectMissingBreedParts(gameVariant, missing)

            // Breeds are missing
            if (missing.size > 0) {
                exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES, missing.joinToString(","))
            }
        }

        task = task.applyBreeds(gameVariant)

        // Set gender
        task = task.withFallbackGender(gender)

        val transform = genome?.getColorTransform(gender, age, genomeVariant)

        task = task.applySwapAndRotation(transform)

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

        if (LocalFileSystem!!.isDirectory(out)) {
            exitNativeWithError(ERROR_CODE__BAD_OUTPUT_FILE, "Output file is required. Found: $out")
        }

        Log.iIf(LOG_DEBUG) { "CLI: " + task.toCLIOpts(false) }

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
        val startI = if (increment) {
            Log.iIf(LOG_VERBOSE) { "Getting incremental start" }
            getIncrementalFileStart(fs, out, poses.count { it.second == null })
        } else {
            0
        }

        Log.iIf(LOG_DEBUG) { "Got Incremental start: $startI" }

        val open = open
        val previousFiles = mutableListOf<String>()

        // Render pose(s)
        val wroteAll = poses.indices.map { i ->
            val pose = poses[i]

            Log.iIf(LOG_VERBOSE) { "Getting out file name" }

            // Get actual file name to write to.
            // This could have been set on the CLI or could be automatically incremented
            val outActual = pose.second // get explicitly set name
                ?.replace("[\\\\/:]".toRegex(), "_")  // Replace illegal chars
                ?: getOutputFileName(
                    fs,
                    out,
                    startI + i,
                    isMultiPose = poses.size > 1,
                    increment = increment,
                    previousFiles = previousFiles
                )
            Log.iIf(LOG_VERBOSE) { "Got out file name: $outActual" }

            previousFiles.add(outActual)

            Triple(i, outActual, pose)

        }.mapAsync { (i, outToTry, pose) ->

            // Actually render the image
            val rendered = renderer(pose.first, scale)
                ?: exitWithError(RENDER_ERROR_CODE__FAILED_TO_RENDER)

            // Convert Bitmap32 to PNG bytes
            val outputBytes = PNG.encode(rendered)

            try {
                fileNameLock.withLock {
                    var outActual = outToTry
                    while (fs.fileExists(outActual) && increment) {
                        outActual = getOutputFileName(
                            fs,
                            out,
                            startI + i,
                            isMultiPose = poses.size > 1,
                            increment = increment,
                            previousFiles = previousFiles
                        )
                    }
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
                exitWithError(RENDER_ERROR_CODE__FAILED_TO_WRITE_IMAGE, out, stackTrace = e.stackTraceToString())
            }
        }.all { it }

        if (usesRandom) {
            logValuesForRandom(task)
        }
        // Write PNG bytes to output file
        return if (wroteAll) 0 else 1
    }

    private fun BreedRendererBuilder.applySwapAndRotation(genomeTransform: PaletteTransform?): BreedRendererBuilder {
        // Modifiable task
        var out = this

        // Validate tint values
        val rgb = getTintValues(tint)

        // Store for non-null checks and use
        var swap = getColorValue("swap", swap)
        if (swap != null && swap < 0) {
            swap = Random.nextInt(0, 256)
        }
        var rotation = getColorValue("rotation", rotation)
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
                red = rgb?.get(0) ?: genomeTransform?.red,
                green = rgb?.get(1) ?: genomeTransform?.green,
                blue = rgb?.get(2) ?: genomeTransform?.blue,
                swap = swap ?: genomeTransform?.swap,
                rotation = rotation ?: genomeTransform?.rotation,
                throwOnInvalidValue = true
            )
        } else {
            out
        }
    }

    private fun toKey(gameVariant: GameVariant, breedKey: BreedKey): BreedKey {
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

    private fun collectMissingBreedParts(gameVariant: GameVariant, missing: MutableList<String>) {
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

    private fun BreedRendererBuilder.applyBreeds(gameVariant: GameVariant): BreedRendererBuilder {
        val default = breed
        val head = head ?: default
        val hair = hair ?: default
        val body = body ?: default
        val legs = legs ?: default
        val arms = arms ?: default
        val tail = tail ?: default

        var out = withHead(toKey(gameVariant, head!!))
            .withBody(toKey(gameVariant, body!!))
            .withArms(toKey(gameVariant, arms!!))
            .withLegs(toKey(gameVariant, legs!!))
        if (tail != null) {
            out = out.withTail(toKey(gameVariant, tail))
        }
        if (hair != null) {
            out = out.withHair(toKey(gameVariant, hair))
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

    private fun logValuesForRandom(task: BreedRendererBuilder) {


        var i = 0
        val transform = task.getPaletteTransform()

        // Console emphasis color
        val bold = BOLD + BLACK_BACKGROUND + WHITE + UNDERLINE_WHITE

        // Rebuilds entire command with concrete values for random placeholders
        val out = StringBuilder(bold)
            .append("Command with random values:\n")
            .append(RESET)
            .append("render-creature")

        // String builder to hold only the random value
        // as opposed to `out`, which holds all text in the command
//        val randomOptionsOnly = StringBuilder(bold)

        // Append values function if value is random
        val onRandom = random@{ arg: String, work: StringBuilder.() -> Unit ->
                // See if value is random
                val next = args.getOrNull(i)
                    ?.lowercase()
                    ?: return@random false // Returns should Continue

                if (!next.contains('*') && !next.contains("rand")) {
                    // not random value
                    out.append(' ').append(arg)
                    return@random true
                }
                i++
                out.bold(bold, work)
                true
            }


        while (i < args.size) {
            // Initial arg
            val arg = args.getOrNull(i++)
                ?: break

            // Add option value if random
            val shouldContinue = when (arg) {
                "--swap" -> onRandom(arg) random@{
                    // Check for concrete value
                    val value = transform?.swap
                        ?: return@random
                    append(' ')
                    append(arg)
                    append(' ')
                    append(value)
                }

                "--rotation" -> onRandom(arg) random@{
                    // Check for concrete value
                    val value = transform?.swap
                        ?: return@random
                    append(' ')
                    append(arg)
                    append(' ')
                    append(value)
                }

                "--tint" -> onRandom(arg) random@{
                    val red = transform?.red ?: 128
                    val green = transform?.green ?: 128
                    val blue = transform?.blue ?: 128

                    append(red).append(':')
                    append(green).append(':')
                    append(blue)
                }
                else -> {
                    out.append(' ').append(arg)
                    true
                }
            }
            if (!shouldContinue) {
                break
            }

        }

        out
            .append(RESET)

        Log.i {
            out.toString()
        }
    }

    private fun StringBuilder.bold(bold: String, work: StringBuilder.() -> Unit): StringBuilder {
        append(bold)
        append(' ')
        work()
        append(' ')
        append(RESET)
        return this
    }

}


private suspend fun constructFileSystem(
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
    val fsPhysical = ScopedFileSystem(sourceRootDirectories)

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
    if (genomeFiles != null) {
        fsPhysical.addSourceRoots(genomeFiles.first)
    }

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
        fsPhysical.addSourceRoot(exportPathQualified)
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