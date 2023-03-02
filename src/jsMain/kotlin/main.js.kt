
import bedalton.creatures.breed.render.cli.runMain
import com.bedalton.common.util.PathUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlin.js.Promise

@JsExport
fun runRenderBreedCLI(): Promise<Boolean> {
    var args = process.argv.toList()

    // ========= USES endsWith() -> because args are absolute paths =====//
    if (args.isNotEmpty() && args[0].lowercase().endsWith("npm")) {
        args = args.drop(1)
    }
    if (args.isNotEmpty() && args[0].lowercase().endsWith("node")) {
        args = args.drop(1)
    }
    if (args.isNotEmpty() && args[0].lowercase().endsWith("nodejs")) {
        args = args.drop(1)
    }
    if (args.isNotEmpty() && PathUtil.isAbsolute(args[0])) {
        args = args.drop(1)
    }
    if (args.isNotEmpty() && args[0].lowercase().endsWith(".exe")) {
        args = args.drop(1)
    }
    if (args.isNotEmpty() && args[0].lowercase().endsWith(".js")) {
        args = args.drop(1)
    }
    if (args.isNotEmpty() && (args[0].endsWith("/breed-renderer") || args[0].endsWith("\\breed-renderer"))) {
        args = args.drop(1)
    }
    return GlobalScope.async {
        val result = runMain(args.toTypedArray())//,  "render-breed")
        result == 0
    }.asPromise()
}