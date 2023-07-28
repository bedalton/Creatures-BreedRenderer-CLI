package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.creatures.sprite.util.PaletteTransform
import kotlin.random.Random

internal fun applySwapAndRotation(
    task: BreedRendererBuilder,
    transformVariant: GameVariant?,
    genomeTransform: PaletteTransform?,
    tintString: String?,
    swapString: String?,
    rotationString: String?
): BreedRendererBuilder {
    // Modifiable task
    var out = task

    // Validate tint values
    val rgb = getTintValues(tintString)

    // Store for non-null checks and use
    var swap = getColorValue("swap", swapString)
    if (swap != null && swap < 0) {
        swap = Random.nextInt(0, 256)
    }
    var rotation = getColorValue("rotation", rotationString)
    if (rotation != null && rotation < 0) {
        rotation = Random.nextInt(0, 256)
    }

    // Apply transform variant if any
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
