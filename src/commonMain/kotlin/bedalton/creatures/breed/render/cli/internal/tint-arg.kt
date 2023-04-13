package bedalton.creatures.breed.render.cli.internal

import bedalton.creatures.breed.render.support.pose.PoseRenderException
import com.bedalton.common.util.isNotNullOrBlank
import kotlin.random.Random


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

    if (tint.isNotNullOrBlank() && tint.lowercase() in randValues) {
        actualTint = "*:*:*"
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