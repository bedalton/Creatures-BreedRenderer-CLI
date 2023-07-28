package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.app.exitNativeWithError
import com.bedalton.common.structs.Pointer
import com.bedalton.common.util.isNotNullOrBlank
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__INVALID_AGE_VALUE
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__INVALID_GENDER_VALUE
import com.bedalton.creatures.breed.render.cli.RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER
import com.bedalton.creatures.breed.render.renderer.BreedRendererBuilder
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.creatures.genetics.genome.Genome
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.Log
import com.bedalton.log.iIf
import kotlin.contracts.contract
import kotlin.random.Random


internal suspend fun buildTaskWithoutSettingTintAndBreeds(
    gameVariant: GameVariant,
    finalSources: SourceFiles,
    age: Pointer<Int?>,
    gender: Pointer<Int?>,
    genomeVariant: Pointer<Int?>,
    ghost: List<Char>,
    hidden: List<Char>,
    ghostAlpha: Double?,
    exactMatch: Boolean?,
    renderGhostPartsBelow: Boolean,
    trim: Boolean?,
    nonIntersectingLimbs: Boolean?,
    scale: Double?,
    padding: Int?,
    setParts: Pointer<Boolean>,
    genomePointer: Pointer<Genome?>
): BreedRendererBuilder {


    // Create initial builder
    var task = BreedRendererBuilder(gameVariant)
        .withSetSourceRoots(finalSources.sources + listOfNotNull(finalSources.getGenomePath()))
        .withGhostParts(ghost)
        .withHiddenParts(hidden.joinToString { "$it" })
        .withGhostAlpha(ghostAlpha)
        .withExactMatch(exactMatch)
        .withRenderGhostPartsBelow(renderGhostPartsBelow)
        .withTrimWhitespace(trim)
        .withNonIntersectingLimbs(nonIntersectingLimbs)
        .withPadding(padding)

    if (scale != null) {
        task = task.withDefaultZoom(scale)
    }
    // Base variables
    var ageActual = age.value
    var genderActual = gender.value
    var genomeVariantActual = genomeVariant.value ?: 0

    if (genderActual == -1) {
        genderActual = (Random.nextInt(0, 99) % 2) + 1
        Log.iIf(LOG_DEBUG) { "Resolving random gender to ${getEggGenderValueGender(genderActual!!)} " }
    }

    // Parse Genome if any
    var genome: Genome? = null
    val genomePath = finalSources.getGenomePath()

    if (genomeVariantActual !in 0..8) {
        Log.e { "Genome variant must be a value 0..8; Found: $genomeVariant; Defaulting to '0'" }
        genomeVariantActual = 0
    }

    if (genomePath.isNotNullOrBlank()) {
        val (genomeTemp, genderIfC2Egg) = readGenome(finalSources, genomePath, genomeVariantActual)
        genome = genomeTemp
        // Apply gender from C2 egg
        if (genderIfC2Egg != null) {
            genderActual = genderFromC2EggGenderValue(genderIfC2Egg, genderActual)
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
        genderActual = genderActual ?: (export.gender?.let { it + 1 })
        ageActual = ageActual ?: export.age
    }


    val missing = mutableListOf<String>()

    if (ageActual == null) {
        missing.add("age")
    }
    if (genderActual == null) {
        missing.add("gender")
    }

    // Age or gender is missing
    if (missing.isNotEmpty()) {
        exitWithError(RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER, missing.joinToString(""))
    }

    applyAge(task, ageActual)
    age.value = ageActual
    applyGender(task, genderActual)
    gender.value = genderActual

    if (genome != null) {
        task = applyGenome(
            task = task,
            genome = genome,
            gender = genderActual,
            age = ageActual,
            genomeVariant = genomeVariantActual
        )
        setParts.value = true
        genomePointer.value = genome
    }
    genomeVariant.value = genomeVariantActual
    return task
}


private fun applyGenome(
    task: BreedRendererBuilder,
    genome: Genome,
    gender: Int,
    age: Int,
    genomeVariant: Int
): BreedRendererBuilder {
    val out = task.withGenomeApplyNow(
        genome,
        gender,
        age,
        if (genome.version < 3) 0 else genomeVariant
    )
    Log.iIf(LOG_DEBUG) { "Applied parts from genome" }
    return out
}

private fun applyAge(
    task: BreedRendererBuilder,
    age: Int?
): BreedRendererBuilder {

    contract {
        returns() implies (age != null)
    }
    if (age == null) {
        exitNativeWithError(RENDER_ERROR_CODE__INVALID_AGE_VALUE, "Age value is required; Use \'--age\'")
    }

    if (age !in 0..6) {
        exitWithError(RENDER_ERROR_CODE__INVALID_AGE_VALUE)
    }

    return task.withAge(age)
}

private fun applyGender(
    task: BreedRendererBuilder,
    gender: Int?
): BreedRendererBuilder {

    contract {
        returns() implies (gender != null)
    }
    // Make sure gender is not null (not null if passed from CLI or set by C2 Egg)
    if (gender == null) {
        exitNativeWithError(RENDER_ERROR_CODE__INVALID_GENDER_VALUE, "Value for option --gender must be provided")
    }
    return task.withFallbackGender(gender)
}
