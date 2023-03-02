package bedalton.creatures.breed.render.cli.internal

import bedalton.creatures.breed.render.support.pose.Mood
import kotlinx.cli.ArgType

val MoodArg = ArgType.Choice<Mood>(
    { value ->
        Mood.valueOf(value.uppercase())
    },
    { mood ->
        mood.name.lowercase()
    }
)