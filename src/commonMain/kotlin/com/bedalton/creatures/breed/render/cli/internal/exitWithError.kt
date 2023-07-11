package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.app.exitNativeWithError
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.Log

internal fun exitWithError(code: Int, vararg args: Any, stackTrace: String? = null): Nothing {
    if (Log.hasMode(LOG_DEBUG) && stackTrace != null) {
        exitNativeWithError(code, getErrorMessage(code, *args) + "\n$stackTrace")
    } else {
        exitNativeWithError(code, getErrorMessage(code, *args) + (stackTrace?.let { "\n$it" } ?: ""))
    }
}