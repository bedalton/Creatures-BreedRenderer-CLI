package bedalton.creatures.breed.render.cli

import bedalton.creatures.breed.render.cli.internal.*
import bedalton.creatures.breed.render.cli.internal.GenderArg
import bedalton.creatures.breed.render.cli.internal.PartArg
import bedalton.creatures.breed.render.cli.internal.PartsArg
import bedalton.creatures.breed.render.cli.internal.getIncrementalFileStart
import bedalton.creatures.breed.render.cli.internal.getTintValues
import bedalton.creatures.breed.render.cli.internal.resolvePose
import bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import bedalton.creatures.breed.render.support.pose.Pose
import bedalton.creatures.cli.GameArgType
import bedalton.creatures.common.structs.BreedKey
import bedalton.creatures.common.structs.GameVariant
import bedalton.creatures.creature.common.c2.parser.EggFileParser
import bedalton.creatures.genetics.parser.GenomeParser
import com.bedalton.app.exitNativeWithError
import com.bedalton.app.getCurrentWorkingDirectory
import com.bedalton.cli.Flag
import com.bedalton.cli.unescapeCLIPathAndQualify
import com.bedalton.common.coroutines.mapAsync
import com.bedalton.common.util.PathUtil
import com.bedalton.common.util.nullIfEmpty
import com.bedalton.common.util.pathSeparator
import com.bedalton.common.util.stripSurroundingQuotes
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.LOG_VERBOSE
import com.bedalton.log.Log
import com.bedalton.log.iIf
import com.bedalton.vfs.*
import com.soywiz.korim.format.PNG
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlin.random.Random

class RenderEggCommand : Subcommand("render-c2-egg", "Render a creature from an egg") {

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

    private val poses by option(
        ArgType.String,
        "pose",
        "p",
        "The creature pose to render"
    ).multiple()

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
    ).required()

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

    private val exactMatch by option(
        Flag,
        "exact-match",
        "x",
        "Match sprite files exactly (no substituting age or gender) or fail"
    ).default(false)

    private val egg by argument(
        ArgType.String,
        "egg",
        "Egg agent, or C2 egg file"
    )

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

        if (scale !in 1.0..10.0) {
            exitNativeWithError(1, "Scale must be be 1..10 (inclusive); Found: $scale")
        }


        val missing = mutableListOf<String>()

        // Gender
        var gender = gender
        if (gender == -1) {
            gender = (Random(8880).nextInt(0, 99) % 2) + 1
        }

        // Age
        val age = age
        if (age !in 0..6) {
            exitWithError(RENDER_ERROR_CODE__INVALID_AGE_VALUE)
        }

        // Age or gender is missing
        if (missing.isNotEmpty()) {
            exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER, missing.joinToString(""))
        }

        // Collect missing breed information
        collectMissingBreedParts(missing)
        // Breeds are missing
        if (missing.size > 0) {
            exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES, missing.joinToString(","))
        }

        // Get working directory
        val currentWorkingDirectory = getCurrentWorkingDirectory()?.expandTildeIfNecessary()

        // Combine the sources which are both globbed files, and folders
        val sources = getSourceFolders(currentWorkingDirectory)
        Log.iIf(LOG_DEBUG) { "Command running with ${sources.size} source paths;" }

        // Create initial builder
        val task = BreedRendererBuilder(gameVariant)
            .withSourceRoots(sources)
            .withFallbackGender(gender)
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


        val genome = genomePath?.expandTildeIfNecessary()?.let {path ->
            val qualifiedPath = if (!PathUtil.isAbsolute(path)) {
                if (currentWorkingDirectory == null) {
                    throw Exception("Failed to get current working directory to qualify genome path")
                }
                unescapeCLIPathAndQualify(path, currentWorkingDirectory)
                    ?: throw Exception("Faile to qualify genome path; Genome File: $path; Current Directory: $currentWorkingDirectory")
            } else {
                path
            }
            val fs = ScopedFileSystem(listOf(PathUtil.getWithoutLastPathComponent(qualifiedPath)!!))
            if (!fs.fileExists(qualifiedPath)) {
                throw FileNotFoundException(
                    qualifiedPath,
                    "Failed to locate genome file at $genomePath in $currentWorkingDirectory"
                )
            }
            val bytes = when (PathUtil.getExtension(qualifiedPath)) {
                "egg" -> {
                    val egg = fs.readAsStreamReader(qualifiedPath).readAtCurrentPositionReturning {
                        EggFileParser.parse(this)
                    }
                }
                "gen" -> {
                    GenomeParser.parseGenome(fs.read(qualifiedPath))
                }
            }
        }



        // Get out base path. Could be a file or a folder
        val outWithExpandedTilde = out.expandTildeIfNecessary()
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
        val poses = (poses.ifEmpty { listOf(Pose.defaultPose(gameVariant).toPoseString()) })
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

        // Render pose(s)
        val wroteAll = poses.indices.mapAsync { i ->
            val pose = poses[i]

            // Get actual file name to write to.
            // This could have been set on the CLI or could be automatically incremented
            val outActual = pose.second // get explicitly set name
                ?.replace("[\\\\/:]".toRegex(), "_")  // Replace illegal chars
                ?: getFileName(fs, out, startI + i, isMultiPose = poses.size <= 1, increment = increment)

            // Log actual filename
            Log.iIf(LOG_DEBUG) { "writing: $outActual" }

            // Actually render the image
            val rendered = renderer(pose.first, scale)
                ?: exitWithError(RENDER_ERROR_CODE__FAILED_TO_RENDER)

            // Convert Bitmap32 to PNG bytes
            val outputBytes = PNG.encode(rendered)

            try {
                fs.write(outActual, outputBytes)
                delay(120)
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
            swap = random.nextInt(0, 256)
        }
        var rotation = rotation
        if (rotation != null && rotation < 0) {
            rotation = random.nextInt(0, 256)
        }

        // Apply transform variant if any
        val transformVariant = transformVariant
        if (transformVariant != null) {
            out = out.withPaletteTransformVariant(transformVariant)
        }

        // Apply swap and rotate if needed
        return if (rgb != null || swap != null || rotation != null) {
            out.withSwapAndRotate(
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


    private suspend fun getSourceFolders(currentWorkingDirectory: String?): List<String> {

        sources.filter {
            it.startsWith('-') && !(currentWorkingDirectory?.let { cwd -> PathUtil.combine(cwd, it) }
                ?: it).let { qualified ->
                LocalFileSystem!!.fileExists(qualified)
            }
        }.nullIfEmpty()?.also {
            Log.w {
                "Invalid source directories found: [\n\t${it.joinToString("\n\t")}\n]\nWere these meant to be options? Check prefix dashes and spelling"
            }
        }

        // Get all passed in paths qualified
        val rawPaths = sources
            .map {
                val path = it.expandTildeIfNecessary()
                if (PathUtil.isAbsolute(path)) {
                    path
                } else if (currentWorkingDirectory == null) {
                    exitWithError(
                        ERROR_CODE__BAD_INPUT_FILE,
                        "Failed to get current working directory, and not all paths are absolute; Path: $it"
                    )
                } else {
                    PathUtil.combine(currentWorkingDirectory, path)
                }
            }

        // Get concrete folders (i.e. not or before wildcard)
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