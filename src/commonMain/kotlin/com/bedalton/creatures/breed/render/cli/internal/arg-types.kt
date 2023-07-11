package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.creatures.common.structs.BreedKey
import kotlinx.cli.ArgType


@Suppress("SpellCheckingInspection")
internal object PartArg : ArgType<BreedKey>(true) {
    override val description: kotlin.String
        get() = "Format {genus}:{breed_char}. Example: n:a, grendel:d; With gender: {genus}:{gender}:{breed_char}"//\n*The shorthand for GEAT is S as in Shee, not G as in Grendel

    val randomValues = listOf(
        "rand", "rand()",
        "random", "random()",
        "*", "r", "r:*",
    )
    override fun convert(value: kotlin.String, name: kotlin.String): BreedKey {
//        if (value.lowercase() in randomValues) {
//            return BreedKey(breed = 'a' + random.nextInt(0, 26))
//        }
        val split = value.lowercase().split(':')
        if (split.size !in 2..3) {
            return BreedKey()
        }
        val genus = when (split[0]) {
            "n", "norn" -> 0
            "g", "grendel" -> 1
            "e", "ettin" -> 2
            "s", "shee", "geat" -> 3
            else -> throw Exception("Invalid genus from value: $value")
        }
        var key = BreedKey(
            genus = genus
        )
        if (split.size == 3) {
            val gender = when (split[1].lowercase()) {
                "m", "male", "mal" -> 0
                "f", "female", "fem" -> 1
                else -> throw Exception("Invalid part gender in value: $value; Expected [m]ale or [f]emale")
            }
            key = key.copyWithGender(gender)
        }
        val breedString = if (split.size == 2) split[1] else split[2]
        if (breedString.length != 1) {
            throw Exception("Invalid part breed in value $value. Expected a single char")
        }

//        return if (breedString[0] == '*') {
//            key
//        } else {
//            key.copyWithBreed(breedString[0])
//        }
        return key.copyWithBreed(breedString[0])
    }

}



@Suppress("SpellCheckingInspection")
internal object GenderArg : ArgType<Int>(true) {
    override val description: kotlin.String
        get() = "Gender: [F]emale or [M]ale"

    override fun convert(value: kotlin.String, name: kotlin.String): kotlin.Int {
        return when (value.lowercase()) {
            "?", "any", "rand", "random", "*", "-1", "0" -> -1
            "m", "male", "1" -> 0
            "f", "female", "2" -> 1
            "3" -> /* Norn Binary */ -1 // Set to random
            else -> throw Exception("Invalid gender specified; Expected: [m]ale or [f]emale; Found: $value")
        }
    }

}




internal object PartsArg : ArgType<List<Char>>(true) {
    override val description: kotlin.String
        get() = ""

    override fun convert(value: kotlin.String, name: kotlin.String): List<Char> {
        return value.toCharArray().filter { it in 'a'..'q' }
    }
}