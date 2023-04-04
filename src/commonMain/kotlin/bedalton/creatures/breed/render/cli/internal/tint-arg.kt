package bedalton.creatures.breed.render.cli.internal

import bedalton.creatures.breed.render.support.pose.PoseRenderException
import kotlin.random.Random


/**
 * Break up a tint string argument
 * Format: {r}:{g}:{b}
 */
internal fun getTintValues(tint: String?): List<Int?>? {
    val parts = tint?.split(':')
        ?: return null
    if (parts.size > 3) {
        throw PoseRenderException("Too many color arguments for tint. Expected 3; Found: ${parts.size}")
    }
    return parts.mapIndexed(::getTintValue)
}

/**
 * Convert a text value into a tint value or null
 */
private fun getTintValue(index: Int, stringValue: String): Int? {
    if (stringValue.isEmpty()) {
        return null
    }
    val value = try {
        stringValue.toInt()
    } catch (_: Exception) {
        val color = when (index) {
            0 -> "red"
            1 -> "green"
            2 -> "blue"
            else -> "<Unknown>"
        }
        throw PoseRenderException("Invalid $color color value. Expected int 0..255")
    }
    return if (value < 0) {
        Random.nextInt(0, 256)
    } else if (value !in 0..255) {
        val color = when (index) {
            0 -> "red"
            1 -> "green"
            2 -> "blue"
            else -> "<Unknown>"
        }
        throw PoseRenderException("Invalid $color color value <$value>. Expected int 0..255")
    } else {
        value
    }
}