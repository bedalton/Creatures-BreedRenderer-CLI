package com.bedalton.creatures.breed.render.cli

import com.bedalton.app.exitNativeWithError
import com.bedalton.app.getCurrentWorkingDirectory
import com.bedalton.cli.Flag
import com.bedalton.cli.promptYesNo
import com.bedalton.common.structs.Pointer
import com.bedalton.creatures.breed.render.cli.internal.*
import com.bedalton.creatures.breed.render.renderer.getColorTransform
import com.bedalton.creatures.cli.GameArgType
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.creatures.genetics.genome.Genome
import com.bedalton.log.*
import com.bedalton.log.ConsoleColors.BOLD
import com.bedalton.log.ConsoleColors.RESET
import com.bedalton.vfs.*
import kotlinx.cli.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    ).default(false)

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
        description = "Color rotation shifting. 0-255, with 128 being no change"//. Value less than 128 rotated r<-g<-b. Greater than 128 r->g->b
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


    private val open by option(
        Flag,
        "open",
        description = "Attempt to open rendered image in default image program"
    ).default(false)

    private val usesRandom by lazy {
        val colorValues = ((tint ?: "") + (swap ?: "") + (rotation ?: ""))
        colorValues.contains('*') || colorValues.contains("rand")
    }

    private val shouldPrintAlterGenomeCommand by option(
        Flag,
        fullName = "print-alter-genome-command",
        description = "Print an equivalent alter genome statement minus the filename"
    ).default(false)

    private val loop by option(
        Flag,
        fullName = "loop",
        description = "Loop random generations, until use writes print or exit or hits ctrl+c"
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

        if (debug || verbose) {
            Log.setMode(LOG_DEBUG, true)
        }

        if (verbose) {
            Log.setMode(LOG_VERBOSE, true)
        }

        if (scale !in 1.0..10.0) {
            exitNativeWithError(1, "Scale must be be 1..10 (inclusive); Found: $scale")
        }

        // Get working directory
        val currentWorkingDirectory = getCurrentWorkingDirectory()
            ?.expandTildeIfNecessary() // Needed for Ubuntu
            ?.stripDotSlash()

        // Combine the sources which are both globbed files, and folders
        val sources = getSourceFolders(sources, currentWorkingDirectory)

        // Ensure not using both genome path AND export path
        if (genomePath != null && exportPath != null) {
            exitNativeWithError(1, "Genome and Export options cannot be used together")
        }

        // Construct file system and full paths from CLI arguments and options
        val finalSources = constructFileSystem(sources, currentWorkingDirectory, genomePath, exportPath)

        val gameVariant = gameVariant
            ?: finalSources.findGameVariant().also {
                Log.iIf(LOG_DEBUG) { "Ascertained variant to be $it" }
            }

        val randomPoses = Pointer(false)

        val poses = parsePoseStrings(
            gameVariant,
            poses,
            randomPoses,
            mood,
            eyesClosed
        )

        // Get the output file system needed for writing files
        // This is different from input file system
        val (outputFileSystem, getOutputFile) = getFileSystemSupport(
            finalSources,
            out,
            currentWorkingDirectory,
            poses,
            increment,
            poses.size > 1,
        )

        val age = Pointer(age)
        val gender = Pointer(gender)
        val genomeVariant = Pointer<Int?>(genomeVariant)
        val genomePointer: Pointer<Genome?> = Pointer(null)

        val setParts = Pointer(false)

        var task = buildTaskWithoutSettingTintAndBreeds(
            gameVariant = gameVariant,
            finalSources = finalSources,
            age = age,
            gender = gender,
            genomeVariant = genomeVariant,
            ghost = ghost.flatten(),
            hidden = hidden.flatten(),
            ghostAlpha = ghostAlpha,
            exactMatch = exactMatch,
            renderGhostPartsBelow = renderGhostPartsBelow,
            trim = trim,
            nonIntersectingLimbs = nonIntersectingLimbs,
            scale = scale,
            padding = padding,
            setParts = setParts,
            genomePointer = genomePointer
        )


        // Get color transform from genome if any
        val genomeTransform = genomePointer.value?.getColorTransform(
            gender.value!!, // Must be non-null after buildTask
            age.value!!,
            genomeVariant.value
        )

        // Set colors overwriting genome transform as needed
        task = applySwapAndRotation(
            task = task,
            transformVariant = transformVariant,
            genomeTransform = genomeTransform,
            tintString = tint,
            swapString = swap,
            rotationString = rotation
        )

        task = applyBreeds(
            task = task,
            gameVariant = gameVariant,
            gender = gender.value,
            age = age.value,
            default = breed,
            head = head,
            body = body,
            legs = legs,
            arms = arms,
            tail = tail,
            hair = hair,
            allowNull = setParts.value
        )

        Log.iIf(LOG_DEBUG) { "CLI: " + task.toCLIOpts(false) }

        // Get the actual pose renderer
        val renderer = task.poseRenderer()

        val wroteAll = renderPoses(
            outputFileSystem,
            open,
            poses,
            getOutputFile,
            renderer,
        )

        if (usesRandom) {
            logValuesForRandom(task, poses.map { it.first }, args)
        }

        if (loop) {
            if (!usesRandom && !randomPoses.value) {
                Log.e { "Cannot loop rendering without random colors or poses" }
            } else {
                val renderAgain = promptYesNo(
                    "${BOLD}Continue rendering [Default=True], type \"n\" or \"no\" to stop${RESET}",
                    appendValidResponses = false,
                    default = true
                ).should

                if (renderAgain) {
                    return executeWithResult()
                }
            }
        }
        if (shouldPrintAlterGenomeCommand) {
            logAlterGenomeCommandText(task, gameVariant, genomePath)
        }

        // Write PNG bytes to output file
        return if (wroteAll) 0 else 1
    }


}

