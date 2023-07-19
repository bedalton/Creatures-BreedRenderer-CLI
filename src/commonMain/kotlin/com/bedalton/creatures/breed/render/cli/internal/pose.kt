package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.breed.render.support.pose.Mood
import com.bedalton.creatures.breed.render.support.pose.Pose
import com.bedalton.creatures.breed.render.support.pose.PoseFacing
import com.bedalton.creatures.breed.render.support.pose.PoseStringUtil
import com.bedalton.creatures.common.structs.GameVariant

internal val randomPoseRegex = "(rand(?:om)?)([(-:=].+[)]?)?".toRegex(RegexOption.IGNORE_CASE)

internal fun resolvePose(variant: GameVariant, pose: String, mood: Mood?, eyesClosed: Boolean?): Pose {
    val randomValues = randomPoseRegex
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
            when (pose.lowercase()) {
                "left", "l" -> Pose.defaultPose(variant)
                    .copy(
                        headFacing = PoseFacing.VIEWER_LEFT,
                        bodyFacing = PoseFacing.VIEWER_LEFT,
                        mood = mood,
                        eyesClosed = eyesClosed
                    )

                "right", "r" -> Pose.defaultPose(variant)
                    .copy(
                        headFacing = PoseFacing.VIEWER_RIGHT,
                        bodyFacing = PoseFacing.VIEWER_RIGHT,
                        mood = mood,
                        eyesClosed = eyesClosed
                    )

                "front", "f", "forward" -> Pose.defaultPose(variant)
                    .copy(
                        headFacing = PoseFacing.FRONT,
                        bodyFacing = PoseFacing.FRONT,
                        mood = mood,
                        eyesClosed = eyesClosed
                    )
                "back", "b", "backwards", "away", "a" -> Pose.defaultPose(variant)
                    .copy(
                        headFacing = PoseFacing.BACK,
                        bodyFacing = PoseFacing.BACK,
                        mood = mood,
                        eyesClosed = eyesClosed
                    )

                else -> PoseStringUtil.fromPoseString(variant, pose)
                    .copy(
                        mood = mood,
                        eyesClosed = eyesClosed == true
                    )
            }
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