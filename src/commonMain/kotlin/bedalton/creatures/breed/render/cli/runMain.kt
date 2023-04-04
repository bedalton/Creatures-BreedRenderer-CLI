package bedalton.creatures.breed.render.cli

import com.bedalton.app.exitNative
import com.bedalton.app.exitNativeOk
import com.bedalton.app.exitNativeWithError
import com.bedalton.app.setIsCLI
import com.bedalton.common.util.className
import com.bedalton.common.util.like
import com.bedalton.log.Log

suspend fun runMain(args: Array<String>): Int {
    setIsCLI(true)
    val parser = RenderBreedCommand().apply {
        useDefaultHelpShortName = false
    }
    if (args.any { it like "--debug" }) {
        Log.i { "args(${args.size}):\n\t- " + args.joinToString("\n\t- ") }
    }
    return try {
        parser.parse(args)
        val result = parser.executeWithResult()
        Log.i { "Done.. Result: $result" }
        result
    } catch (e: IllegalStateException) {
        if (e.message?.trim() like "Not implemented for JS!") {
            Log.i { "..." }
            exitNativeOk()
        }
        Log.e { "Hmmn... ${e.message}" }
        exitNative(5000)
    } catch (e: Exception) {
        exitNativeWithError(1) {
            val stack = if (Log.hasMode("debug") || e.message.isNullOrBlank()) {
                "\n" + e.stackTraceToString()
            } else {
                ""
            }
            e.className + (e.message?.let {": $it"} ?: "") + stack
        }
    } catch (e: Error) {
        exitNativeWithError(1) {
            val stack = if (Log.hasMode("debug") || e.message.isNullOrBlank()) {
                "\n" + e.stackTraceToString()
            } else {
                ""
            }
            e.className + (e.message?.let {": $it"} ?: "") + stack
        }
    }
}