package com.bedalton.creatures.breed.render.cli.internal


internal fun String.stripDotSlash(): String {
    return if (startsWith("./")) {
        substring(2)
    } else {
        this
    }
}


internal fun getEggGenderValueGender(genderInt: Int): String? {
    return when (genderInt) {
        0 -> "Random"
        1 -> "Male"
        2 -> "Female"
        3 -> "Norn Binary"
        else -> null
    }
}