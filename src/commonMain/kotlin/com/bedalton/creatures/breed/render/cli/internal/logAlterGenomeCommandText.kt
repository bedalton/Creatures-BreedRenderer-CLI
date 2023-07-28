package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.util.nullIfEmpty
import com.bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import com.bedalton.creatures.common.structs.BreedKey
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.creatures.common.util.getGenusString
import com.bedalton.log.ConsoleColors
import com.bedalton.log.Log



internal fun logAlterGenomeCommandText(
    task: BreedRendererBuilder,
    variant: GameVariant?,
    genomePath: String?,
) {
    val out = StringBuilder(ConsoleColors.BOLD)
        .append(ConsoleColors.BLACK_BACKGROUND)
        .append(ConsoleColors.WHITE)
        .append(ConsoleColors.UNDERLINE_WHITE)
        .append("AlterGenomeCommand:\nrequires appending output file name\nMust ensure path to breed-util is valid\n")
        .append(ConsoleColors.RESET)
        .append("breed-util alter-genome ")

    genomePath.nullIfEmpty()?.also {
        out
            .append(" \"")
            .append(it)
            .append('"')

    }

    val appendBreed = append@{ part: String, breed: BreedKey? ->
        if (breed?.genus == null || breed.breed == null) {
            return@append false
        }
        val genusInt = breed.genus!!
        val breedChar = breed.breed!!

        val genus = getGenusString(genusInt, variant)
            ?.lowercase()
            ?: return@append false
        out
            .append(' ')
            .append("--")
            .append(part)
            .append(' ')
            .append(genus)
            .append(':')
            .append(breedChar)
    }

    val headBreed = task.getHeadBreed()
    val bodyBreed = task.getBodyBreed()
    val legBreed = task.getLegBreed()
    val armBreed = task.getArmBreed()
    val tailBreed = task.getTailBreed()
    val hairBreed = task.getHairBreed()

    val allBreeds = listOfNotNull(
        headBreed,
        bodyBreed,
        legBreed,
        armBreed,
        tailBreed,
        hairBreed
    ).distinct()

    if (allBreeds.isNotEmpty()) {
        if (allBreeds.size == 1) {
            appendBreed("breed", allBreeds[0])
        } else {
            appendBreed("head", headBreed)
            appendBreed("body", bodyBreed)
            appendBreed("legs", legBreed)
            appendBreed("arms", armBreed)
            appendBreed("tail", tailBreed)
            appendBreed("hair", hairBreed)
        }
    }


    val appendColor = append@{ color: String, value: Int? ->
        if (value == null) {
            return@append
        }
        out
            .append(' ')
            .append("--")
            .append(color)
            .append(' ')
            .append(value)
    }

    task.getPaletteTransform()?.also { transform ->
        appendColor("red", transform.red)
        appendColor("green", transform.green)
        appendColor("blue", transform.blue)
        appendColor("swap", transform.swap)
        appendColor("rotation", transform.rotation)
    }

    Log.i(out::toString)
}