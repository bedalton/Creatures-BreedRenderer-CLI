package bedalton.creatures.breed.render.cli.internal

import bedalton.creatures.breed.render.support.pose.Mood
import bedalton.creatures.breed.render.support.pose.Pose
import bedalton.creatures.breed.render.support.pose.PoseFacing
import bedalton.creatures.breed.render.support.pose.PoseStringUtil
import bedalton.creatures.common.structs.GameVariant

internal fun resolvePose(variant: GameVariant, pose: String, mood: Mood?, eyesClosed: Boolean?): Pose {
    val randomValues = "rand(om)?([(-:].+[)]?)?".toRegex(RegexOption.IGNORE_CASE)
        .matchEntire(pose)
        ?.groupValues
        ?.drop(1)
    return when (randomValues?.size) {
        1 -> randomPose(variant)
        2 -> {
            val direction = getDirectionForRandomPose(randomValues[1])
            randomPose(variant, direction).copy(
                mood = mood,
                eyesClosed = eyesClosed
            )
        }
        else -> {
            PoseStringUtil.fromPoseString(variant, pose)
                .copy(
                    mood = mood,
                    eyesClosed = eyesClosed == true
                )
        }
    }
}

private fun getDirectionForRandomPose(directionString: String): PoseFacing? {
    @Suppress("SpellCheckingInspection")
    return when (directionString.replace("[^a-z]+", "").lowercase()) {
        "left", "l", "west", "3" -> PoseFacing.VIEWER_LEFT
        "right", "r", "east", "2" -> PoseFacing.VIEWER_RIGHT
        "front", "forward", "f", "south", "1" -> PoseFacing.FRONT
        "back", "away", "north", "0" -> PoseFacing.BACK
        "leftright", "rightleft", "leftorright", "rightorleft", "rl", "lr", "eastwest", "westeast", "23", "32" -> {
            if (randomTrueFalse) {
                PoseFacing.VIEWER_LEFT
            } else {
                PoseFacing.VIEWER_RIGHT
            }
        }

        "frontback", "backfront", "frontorback", "backorfront", "bf", "fb", "southnorth", "northsouth", "southornorth", "northorsouth", "01", "10" -> {
            if (randomTrueFalse) {
                PoseFacing.FRONT
            } else {
                PoseFacing.BACK
            }
        }

        else -> null
    }
}