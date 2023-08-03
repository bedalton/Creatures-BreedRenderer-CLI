package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.breed.render.support.pose.*
import com.bedalton.creatures.common.structs.GameVariant
import com.bedalton.creatures.common.structs.isC1e
import kotlin.random.Random

val randomTrueFalse get() = (Random.nextInt(0, 100) % 2) == 0


val C1E_Front: Pose = PoseStringUtil.fromPoseString(GameVariant.C1, "140000000000000")
val C2E_FRONT: Pose = PoseStringUtil.fromPoseString(GameVariant.C3, "113122122111111")

internal fun randomEemFooPose(variant: GameVariant, gender: Int, age: Int): Pose {
    if (variant == GameVariant.C1) {
        if (age >= 2) {
            return C1E_Front
        }
    } else if (variant == GameVariant.C2) {
        if (age >= 3) {
            return C1E_Front
        }
    } else {
        if (age >= 4) {
            return C2E_FRONT
        }
    }
    val pose = if (gender == 1) {
        when (Random.nextInt(0, 3)) {
            0 -> "323322111013311"
            1 -> "313322100111211"
            else -> "342010222032211"
        }
    } else {
        when (Random.nextInt(0, 3)) {
            0 -> "243322100210311"
            1 -> "212010222220011"
            else -> "233020332330011"
        }
    }
    return PoseStringUtil.fromPoseString(variant, pose)
}

internal fun randomPose(variant: GameVariant, facingConstraint: PoseFacing? = null): Pose {
    val bodyFacing = facingConstraint ?: (when (Random.nextInt(0, 9)) {
        0, 1 -> PoseFacing.FRONT
        2 -> PoseFacing.BACK
        3, 4, 5 -> PoseFacing.VIEWER_LEFT
        6, 7, 8 -> PoseFacing.VIEWER_RIGHT
        else -> throw PoseException("Invalid random facing direction")
    })
    val headAgrees = (Random.nextInt(0, 100) % 2) == 0
    val headFacing = if (headAgrees) {
        bodyFacing
    } else {
        when (bodyFacing) {
            PoseFacing.FRONT -> {
                if (randomTrueFalse) {
                    PoseFacing.VIEWER_LEFT
                } else {
                    PoseFacing.VIEWER_RIGHT
                }
            }

            PoseFacing.BACK -> PoseFacing.BACK
            else -> {
                // Face should be way more likely to face front
                val faceFront = (Random.nextInt(0, 100) % 5) > 0
                if (faceFront) {
                    PoseFacing.FRONT
                } else {
                    PoseFacing.BACK
                }
            }
        }
    }

    val body = (when (Random.nextInt(0, 8)) {
        0 -> Tilt.DOWN
        1 -> Tilt.STRAIGHT
        2, 3, 4 -> Tilt.UP
        5, 6, 7 -> Tilt.FAR_UP
        else -> throw PoseException("Invalid random facing direction")
    })
    val head = getRandomTilt()
    val (leftThigh, leftShin, leftFoot) = getLeg()
    val (rightThigh, rightShin, rightFoot) = getLeg()
    val (leftUpperArm, leftForearm) = getArm()
    val (rightUpperArm, rightForearm) = getArm()
    val tailBase = getRandomTilt()
    val tailTip = getRandomTilt()
    val mood = mood(variant)

    return Pose(
        variant = variant,
        headFacing = headFacing,
        head = head,
        bodyFacing = bodyFacing,
        body = body,
        leftThigh = leftThigh,
        leftShin = leftShin,
        leftFoot = leftFoot,
        rightThigh = rightThigh,
        rightShin = rightShin,
        rightFoot = rightFoot,
        leftUpperArm = leftUpperArm,
        leftForearm = leftForearm,
        rightUpperArm = rightUpperArm,
        rightForearm = rightForearm,
        tailBase = tailBase,
        tailTip = tailTip,
        mood = mood,
        eyesClosed = eyesClosed()
    )

}


private fun getArm(): Pair<Tilt, Tilt> {
    val upperArm = getRandomTilt()
    if (upperArm == Tilt.FAR_UP) {
        return Pair(upperArm, Tilt.FAR_UP)
    } else if (upperArm == Tilt.UP) {
        return Pair(upperArm, Tilt.fromChar('2' + Random.nextInt(0, 2))!!)
    } else if (upperArm == Tilt.STRAIGHT) {
        return Pair(upperArm, Tilt.fromChar('1' + Random.nextInt(0, 3))!!)
    } else if (upperArm == Tilt.DOWN) {
        return Pair(upperArm, getRandomTilt())
    } else {
        throw PoseException("Invalid previous tilt on get arm tilt")
    }
}

private fun eyesClosed(): Boolean = (Random.nextInt(0, 99) % 3) == 2

private fun mood(variant: GameVariant): Mood {
    return if (variant.isC1e) {
        Mood.fromInt(Random.nextInt(0, 4))!!
    } else {
        Mood.fromInt(Random.nextInt(0, 6))!!
    }
}

private fun getLeg(): Triple<Tilt, Tilt, Tilt> {
    val thigh = getRandomTilt()
    val shin = when (thigh) {
        Tilt.FAR_UP -> Tilt.fromChar('1' + Random.nextInt(0, 3))!!
        Tilt.UP, Tilt.STRAIGHT -> getRandomTilt()
        Tilt.DOWN -> Tilt.fromChar('0' + Random.nextInt(0, 3))!!
        else -> throw PoseException("Invalid non-concrete random tilt on thigh")
    }
    val foot = when (shin) {
        Tilt.FAR_UP, Tilt.UP -> getRandomTilt()
        Tilt.STRAIGHT -> Tilt.fromChar('0' + Random.nextInt(0, 3))!!
        Tilt.DOWN -> Tilt.DOWN
        else -> throw PoseException("Invalid non-concrete random tilt on shin")
    }
    return Triple(thigh, shin, foot)
}

private fun getRandomTilt(): Tilt { //earlier: Tilt? = null): Tilt {
    return Tilt.fromChar('0' + Random.nextInt(0, 4))!!
}

