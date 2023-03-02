package bedalton.creatures.breed.render.cli

const val RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER = 1000
const val RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES = 1001
const val RENDER_ERROR_CODE__FAILED_TO_RENDER = 1002
const val RENDER_ERROR_CODE__FAILED_TO_WRITE_IMAGE = 1003
const val RENDER_ERROR_CODE__INVALID_GENDER_VALUE = 1004
const val RENDER_ERROR_CODE__INVALID_AGE_VALUE = 1005
const val RENDER_ERROR_CODE__INVALID_GENUS = 1006
const val RENDER_ERROR_CODE__INVALID_BREED = 1007
const val RENDER_ERROR_CODE__MISSING_POSE = 1008
const val RENDER_ERROR_CODE__INVALID_POSE_STRING = 1009
const val RENDER_ERROR_CODE__INVALID_TINT_VALUE = 1010

internal fun getErrorMessage(code: Int, vararg args: Any, includeCode: Boolean = true): String {

    @Suppress("SpellCheckingInspection")
    val message = when (code) {
        RENDER_ERROR_CODE__MISSING_REQUIRED_AGE_OR_GENDER ->
            "Cannot render creature without setting ${args.getOrNull(0) ?: "--age or --gender"}"
        RENDER_ERROR_CODE__MISSING_REQUIRED_BREED_VALUES ->
            "Cannot render creature without part breeds for ${args.getOrNull(0) ?: "head, body, arms and legs"}; Use --breed, -b to set a fallback breed for all parts"
        RENDER_ERROR_CODE__FAILED_TO_RENDER ->
            "Failed to render breed image"
        RENDER_ERROR_CODE__FAILED_TO_WRITE_IMAGE -> "Failed to write image to ${args[0]}"
        RENDER_ERROR_CODE__INVALID_GENDER_VALUE -> "Invalid gender value. Expect [m]ale, [f]emale"
        RENDER_ERROR_CODE__INVALID_AGE_VALUE -> "Age must be an integer value 0..6 (inclusive)"
        RENDER_ERROR_CODE__INVALID_GENUS -> "Invalid breed genus. Expected [n]orn, [g]rendel, [e]ttin, [s]hee, geat"
        RENDER_ERROR_CODE__INVALID_BREED -> "Invalid breed slot. Expected breed value ${args[0]}..${args[1]}"
        RENDER_ERROR_CODE__MISSING_POSE -> "Cannot render without pose string"
        RENDER_ERROR_CODE__INVALID_POSE_STRING -> "Invalid pose string. Pose string should be 15 characters in length with values first two chars: [0-5, X,?,!] and all others: [0..3]"
        else -> throw Exception("InternalError: No message set for error: $code")
    }
    return if (includeCode) {
        "Error $code: $message"
    } else {
        message
    }
}