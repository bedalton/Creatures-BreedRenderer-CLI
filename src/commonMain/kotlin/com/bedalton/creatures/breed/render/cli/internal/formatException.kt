package com.bedalton.creatures.breed.render.cli.internal

import com.bedalton.common.util.className
import com.bedalton.log.LOG_DEBUG
import com.bedalton.log.Log

fun Exception.formatted(): String {
    val message = message?.let { ": $it" } ?: ""
    val stack = if (Log.hasMode(LOG_DEBUG)) {
        '\n' + stackTraceToString()
    } else {
        ""
    }
    return "${className}$message$stack"
}