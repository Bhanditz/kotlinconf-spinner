import android.*
import kotlinx.cinterop.*
import kotlin.text.*

// Hackaround showing how one could use dynamic libs on most Android versions.
// We explicitly preload "libopenal.so" and then load the actual activity.
val prefix by lazy {
    memScoped {
        val dlinfo = alloc<Dl_info>()
        if (dladdr(staticCFunction { -> }, dlinfo.ptr) != 0 && dlinfo.dli_fname != null) {
            val dli_fname = dlinfo.dli_fname!!.toKString()
            if (dli_fname.indexOf('/') == -1)
                "/data/data/com.jetbrains.konan_activity/lib"
            else
                dlinfo.dli_fname!!.toKString().substringBeforeLast('/')
        } else {
            "."
        }
    }
}

fun loadKonanLibrary(name: String): COpaquePointer? {
    println("Loading $name...")
    val handle = dlopen("$prefix/lib$name.so", RTLD_NOW or RTLD_GLOBAL)
    if (handle == null) {
        println("cannot load $name from $prefix: ${dlerror()!!.toKString()}")
    }
    return handle
}

fun main(args: Array<String>) {
    println("Enter loader")
    loadKonanLibrary("openal")
    val kotlin3d = loadKonanLibrary("kotlin3d")
    if (kotlin3d == null) {
        println("Cannot load main lib...")
        return
    }
    val entry = dlsym(kotlin3d, "Konan_main")?.reinterpret<
            CFunction<(COpaquePointer?, COpaquePointer?, size_t) -> Unit>>()
    if (entry != null) memScoped {
        val state = alloc<NativeActivityState>()
        getNativeActivityState(state.ptr)
        println("Calling entry point")
        entry(state.activity, state.savedState, state.savedStateSize)
    } else {
        println("main entry point not found...")
        return
    }
}