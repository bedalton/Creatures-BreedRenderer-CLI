package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.app.exitNativeWithError
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__INVALID_BREED
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__INVALID_GENUS
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES
import com.bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import com.bedalton.creatures.common.structs.BreedKey
import com.bedalton.creatures.common.structs.GameVariant


internal fun applyBreeds(
    task: BreedRendererBuilder,
    gameVariant: GameVariant,
    gender: Int?,
    age: Int?,
    default: BreedKey?,
    head: BreedKey?,
    body: BreedKey?,
    legs: BreedKey?,
    arms: BreedKey?,
    tail: BreedKey?,
    hair: BreedKey?,
    allowNull: Boolean
): BreedRendererBuilder {
    val headActual = head ?: default
    val hairActual = hair ?: default
    val bodyActual = body ?: default
    val legsActual = legs ?: default
    val armsActual = arms ?: default
    val tailActual = tail ?: default

    val missing = collectMissingBreedParts(
        gameVariant = gameVariant,
        head = headActual,
        body = bodyActual,
        legs = legsActual,
        arms = armsActual,
        tail = tailActual
    )

    if (!allowNull && missing.isNotEmpty()) {
        exitNativeWithError(1) {
            "Missing breed selection for parts: $missing; Use `--breed {genus}:{breed}` to define default breed\n " +
                    "Or define individual parts using: \n\t${missing.joinToString("\n\t") { "--$it {genus}:{breed}" }}`"
        }
    }

    val toKey = { part: BreedKey ->
        toKey(gameVariant, part, gender, age)
    }

    var out = task

    if (headActual != null) {
        out = out.withHead(toKey(headActual))
    }
    if (bodyActual != null) {
        out = out.withBody(toKey(bodyActual))
    }
    if (legsActual != null) {
        out = out.withLegs(toKey(legsActual))
    }
    if (armsActual != null) {
        out = out.withArms(toKey(armsActual))
    }

    if (tailActual != null) {
        out = out.withTail(toKey(tailActual))
    }

    if (hairActual != null) {
        out = out.withHair(toKey(hairActual))
    }
    return out
}




private fun toKey(
    gameVariant: GameVariant,
    breedKey: BreedKey,
    gender: Int?,
    age: Int?
): BreedKey {
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
    if (out.ageGroup == null) {
        out = out.copyWithAgeGroup(age)
    }
    return out
}



private fun collectMissingBreedParts(
    gameVariant: GameVariant,
    head: BreedKey?,
    body: BreedKey?,
    legs: BreedKey?,
    arms: BreedKey?,
    tail: BreedKey?,
): List<String> {

    val missing = mutableListOf<String>()
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
    if (missing.isNotEmpty() && tail == null && gameVariant != GameVariant.C1) {
        missing.add("tail")
    }
    return missing
}
