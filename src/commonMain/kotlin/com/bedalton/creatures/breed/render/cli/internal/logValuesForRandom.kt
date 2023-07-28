package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import com.bedalton.creatures.breed.render.support.pose.Pose
import com.bedalton.log.ConsoleColors
import com.bedalton.log.Log
import com.bedalton.log.ConsoleColors.BLACK_BACKGROUND
import com.bedalton.log.ConsoleColors.BOLD
import com.bedalton.log.ConsoleColors.RESET
import com.bedalton.log.ConsoleColors.UNDERLINE_WHITE
import com.bedalton.log.ConsoleColors.WHITE

internal fun logValuesForRandom(
    task: BreedRendererBuilder,
    poses: List<Pose>,
    args: Array<String>
) {


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


    var pose = -1
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

            "--pose", "-p" -> {
                val value = args.getOrNull(i + 1)
                    ?: break
                if (!randomPoseRegex.matches(value.trim())) {
                    out
                        .append(' ')
                        .append(arg)
                    continue
                }

                val poseString = poses.getOrNull(++pose)
                    ?.toPoseString()
                if (poseString == null) {
                    out.append(' ')
                        .append(arg)
                    continue
                }
                i++
                out.bold(bold) {
                    append(' ')
                    append("--pose")
                    append(' ')
                    append(poseString)
                }
                true
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

    out.append(RESET)

    Log.i {
        out.toString()
    }
}


private fun StringBuilder.bold(bold: String, work: StringBuilder.() -> Unit): StringBuilder {
    append(bold)
    append(' ')
    work()
    append(' ')
    append(ConsoleColors.RESET)
    return this
}