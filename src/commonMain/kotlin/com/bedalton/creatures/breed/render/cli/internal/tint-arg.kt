package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.breed.render.support.pose.PoseRenderException
import com.bedalton.common.util.isNotNullOrBlank
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.Log
import com.bedalton.log.eIf
import com.bedalton.log.iIf
import kotlin.random.Random

private val random = Random(Random.nextInt())

internal val randValues = listOf(
    "rand",
    "random",
    "?",
    "*",
    "-1"
)

/**
 * Break up a tint string argument
 * Format: {r}:{g}:{b}
 */
internal fun getTintValues(tint: String?): List<Int?>? {
    var actualTint = tint?.lowercase()
        ?: return null

    if (tint.isNotNullOrBlank()) {
        if (tint.lowercase() in randValues) {
            actualTint = "*:*:*"
        }
        if (tint.startsWith('#')) {
            actualTint = tint.substring(1)
        }
        if (actualTint.all { it in 'a'..'f' || it in '0'..'9' }) {
            try {
                if (actualTint.length == 3) {
                    val red = "${actualTint[0]}${actualTint[0]}".toInt(16)
                    val green = "${actualTint[1]}${actualTint[1]}".toInt(16)
                    val blue = "${actualTint[2]}${actualTint[2]}".toInt(16)
                    actualTint = "$red:$green:$blue"
                } else if (actualTint.length == 6){
                    val red = "${actualTint[0]}${actualTint[1]}".toInt(16)
                    val green = "${actualTint[2]}${actualTint[3]}".toInt(16)
                    val blue = "${actualTint[4]}${actualTint[5]}".toInt(16)
                    actualTint = "$red:$green:$blue"
                }
            } catch (_: Exception) {
                Log.eIf(LOG_DEBUG) { "Tint was thought to be HEX color, but failed parse; Value: $tint" }
            }

        }
    }
    val parts = actualTint.split(':')
    if (parts.size > 3) {
        throw PoseRenderException("Too many color arguments for tint. Expected 3; Found: ${parts.size}")
    }
    return parts.mapIndexed(::getTintValue)
}


/**
 * Convert a text value into a tint value or null
 */
private fun getTintValue(index: Int, stringValue: String): Int? {
    val color = when (index) {
        0 -> "red"
        1 -> "green"
        2 -> "blue"
        else -> "<Unknown>"
    }
    return getColorValue(color, stringValue)
}

/**
 * Convert a text value into a tint value or null
 */
internal fun getColorValue(colorName: String, stringValue: String?): Int? {
    if (stringValue.isNullOrBlank()) {
        return null
    }
    val value = try {
        if (stringValue in randValues) {
            Random.nextInt(0, 255)
        } else {
            stringValue.toInt()
        }
    } catch (_: Exception) {
        throw PoseRenderException("Invalid $colorName color value. Expected int 0..255; Found: $stringValue")
    }
    return if (value < 0) {
        Random.nextInt(0, 256)
    } else if (value !in 0..255) {
        throw PoseRenderException("Invalid $colorName color value <$value>. Expected int 0..255; Found: $stringValue")
    } else {
        value
    }
}