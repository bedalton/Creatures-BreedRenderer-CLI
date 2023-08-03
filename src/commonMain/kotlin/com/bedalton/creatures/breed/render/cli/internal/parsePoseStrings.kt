package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.structs.Pointer
import com.bedalton.common.util.stripSurroundingQuotes
import com.bedalton.creatures.breed.render.support.pose.Mood
import com.bedalton.creatures.breed.render.support.pose.Pose
import com.bedalton.creatures.common.structs.GameVariant


// Create RegEx for finding if a pose has an associated file name
private val poseWithNameRegex = "([0-5Xx!?]{15})[:,\\-=](.*)".toRegex()
internal val EEMFOO_PLACEHOLDER = "@@@EEEEMFOOOOO@@"

/**
 * Parses a raw list of pose strings and possibly joined with filename
 * into pairs of pose objects and possibly filenames
 * Format is {pose string}={filename} or {pose string}:{filename}
 */
internal fun parsePoseStrings(
    gameVariant: GameVariant,
    poseStrings: List<String>,
    randomPoses: Pointer<Boolean>,
    mood: Mood?,
    eyesClosed: Boolean?,
): List<Pair<Pose, String?>> {

    if (poseStrings.isEmpty()) {
        return listOf()
    }
    val poseStringsNotEmpty = poseStrings.ifEmpty {
        listOf(Pose.defaultPose(gameVariant).toPoseString())
    }
    return poseStringsNotEmpty.map { poseIn ->
        if (poseIn.lowercase() == "eemfoo") {
            return@map Pose.defaultPose(gameVariant) to EEMFOO_PLACEHOLDER
        }
        parsePoseString(
            gameVariant = gameVariant,
            poseIn = poseIn,
            randomPoses = randomPoses,
            mood = mood,
            eyesClosed = eyesClosed
        )
    }
}

private fun parsePoseString(
    gameVariant: GameVariant,
    poseIn: String,
    randomPoses: Pointer<Boolean>,
    mood: Mood?,
    eyesClosed: Boolean?,
): Pair<Pose, String?> {
    val temp = poseWithNameRegex
        .matchEntire(poseIn.stripSurroundingQuotes())
        ?.groupValues
        ?.drop(1)
    val pose = temp?.getOrNull(0) ?: poseIn
    if (randomPoseRegex.matches(pose.trim() ?: "")) {
        randomPoses.value = true
    }
    val fileName = temp?.getOrNull(1)
    return Pair(resolvePose(gameVariant, pose, mood, eyesClosed), fileName)
}