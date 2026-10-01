package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.io.FileNotFoundException
















internal class HidePathDefender(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    private lateinit var paths: List<String>

    fun install() {
        val list = snapshot().hidePaths
        if (list.isEmpty()) return
        paths = list
        hookFile()
        hookStreams()
        hookNio()
        logInfo("hide path installed (${list.size} paths)")
    }

    private fun hit(path: String?): Boolean {
        if (path.isNullOrEmpty()) return false
        val p = path.trim()
        if (p.isEmpty()) return false
        for (h in paths) {
            if (p == h) return true
            if (p.startsWith("$h/")) return true
        }
        return false
    }

    private fun pathOf(self: Any?): String? {
        if (self == null) return null
        return runCatching {
            self.javaClass.getMethod("getPath").invoke(self) as? String
        }.getOrNull() ?: runCatching {
            self.javaClass.getMethod("getAbsolutePath").invoke(self) as? String
        }.getOrNull()
    }

    

    private val FALSE_METHODS = setOf(
        "exists", "isFile", "isDirectory", "isHidden",
        "canRead", "canWrite", "canExecute",
        "mkdir", "mkdirs", "createNewFile", "delete",
        "setReadOnly", "isAbsolute",
    )
    private val ZERO_METHODS = setOf("length", "lastModified", "getFreeSpace", "getTotalSpace", "getUsableSpace")

    private fun hookFile() {
        val file = loadClassAnywhere("java.io.File") ?: return

        file.declaredMethods.forEach { m ->
            if (m.parameterTypes.isNotEmpty()) return@forEach
            when {
                m.name in FALSE_METHODS && m.returnType == java.lang.Boolean.TYPE -> {
                    hookMethod(m) { chain ->
                        if (hit(pathOf(chain.getThisObject()))) false else chain.proceed()
                    }
                }

                m.name in ZERO_METHODS && m.returnType == java.lang.Long.TYPE -> {
                    hookMethod(m) { chain ->
                        if (hit(pathOf(chain.getThisObject()))) 0L else chain.proceed()
                    }
                }

                m.name == "list" && m.returnType == Array<String>::class.java -> {
                    hookMethod(m) { chain ->
                        if (hit(pathOf(chain.getThisObject()))) null else chain.proceed()
                    }
                }

                m.name == "listFiles" && m.returnType == Array<java.io.File>::class.java -> {
                    hookMethod(m) { chain ->
                        if (hit(pathOf(chain.getThisObject()))) null else chain.proceed()
                    }
                }
            }
        }
        logInfo("file query hooks installed")
    }

    

    private fun hookStreams() {
        val targets = listOf(
            "java.io.FileInputStream",
            "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            "java.io.FileReader",
            "java.io.FileWriter",
        )
        targets.forEach { name ->
            val c = loadClassAnywhere(name) ?: return@forEach
            c.declaredConstructors.forEach { ctor ->
                val ps = ctor.parameterTypes
                if (ps.isEmpty()) return@forEach
                val first = ps[0].name
                if (first != "java.lang.String" && first != "java.io.File") return@forEach
                runCatching {
                    deopt(ctor)
                    module.hook(ctor)
                        .setPriority(XposedInterface.PRIORITY_HIGHEST)
                        .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                        .intercept { chain ->
                            val arg = chain.getArg(0)
                            val p = if (first == "java.lang.String") arg as? String else pathOf(arg)
                            if (hit(p)) {
                                throw FileNotFoundException("$p: No such file or directory")
                            }
                            chain.proceed()
                        }
                }
            }
        }
        logInfo("stream ctor hooks installed")
    }

    

    private val NIO_FALSE = setOf(
        "exists", "notExists", "isRegularFile", "isDirectory",
        "isReadable", "isWritable", "isExecutable", "isHidden", "isSameFile",
    )

    private fun hookNio() {
        val files = loadClassAnywhere("java.nio.file.Files") ?: return
        val pathCls = runCatching { Class.forName("java.nio.file.Path") }.getOrNull() ?: return

        files.declaredMethods.forEach { m ->
            val ps = m.parameterTypes
            if (ps.isEmpty() || ps[0] != pathCls) return@forEach
            when (m.name) {
                in NIO_FALSE -> {
                    if (m.returnType == java.lang.Boolean.TYPE) {
                        hookMethod(m) { chain ->
                            if (hit(chain.getArg(0)?.toString())) {
                                
                                m.name == "notExists"
                            } else {
                                chain.proceed()
                            }
                        }
                    }
                }

                "size" -> hookMethod(m) { chain ->
                    if (hit(chain.getArg(0)?.toString())) 0L else chain.proceed()
                }

                "probeContentType" -> hookMethod(m) { chain ->
                    if (hit(chain.getArg(0)?.toString())) null else chain.proceed()
                }
            }
        }

        
        listOf("newInputStream", "newByteChannel", "newBufferedReader", "readAllBytes")
            .forEach { name ->
                files.declaredMethods.filter { it.name == name && it.parameterTypes.firstOrNull() == pathCls }
                    .forEach { m ->
                        runCatching {
                            deopt(m)
                            module.hook(m)
                                .setPriority(XposedInterface.PRIORITY_HIGHEST)
                                .setExceptionMode(XposedInterface.ExceptionMode.PASSTHROUGH)
                                .intercept { chain ->
                                    if (hit(chain.getArg(0)?.toString())) {
                                        throw FileNotFoundException("No such file or directory")
                                    }
                                    chain.proceed()
                                }
                        }
                    }
            }
        logInfo("nio hooks installed")
    }
}
