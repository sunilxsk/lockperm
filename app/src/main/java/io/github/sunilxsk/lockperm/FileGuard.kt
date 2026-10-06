package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import android.net.Uri
import io.github.libxposed.api.XposedModule




















internal class FileGuard(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    
    private val PUBLIC_PREFIXES = listOf("/sdcard", "/storage", "/mnt", "/emulated")

    fun installNow() {
        XpState.Flags.forceFileGuard = true
        install()
    }

    private fun on(): Boolean =
        XpState.Flags.forceFileGuard || snapshot().fileGuardEnable

    fun install() {
        if (!on()) return
        hookContentResolver()
        hookJavaFile()
        hookNioFiles()
        logInfo("file guard installed")
    }

    



    private fun on(key: String): Boolean {
        
        if (XpState.Flags.forceFileGuard) return true
        val cfg = snapshot()
        if (!cfg.fileGuardEnable) return false
        return cfg.fileOpAll || key in cfg.fileOps
    }

    

    private fun hookContentResolver() {
        val cr = cls("android.content.ContentResolver")
            ?: runCatching { Class.forName("android.content.ContentResolver") }.getOrNull()
            ?: return

        cr.declaredMethods.forEach { m ->
            when (m.name) {
                "insert" -> hookMethod(m) { chain ->
                    if (on(XpConfig.KEY_FILE_OP_INSERT) && isMediaUri(chain)) {
                        logWarn("blocked ContentResolver.insert (create file)")
                        null
                    } else {
                        chain.proceed()
                    }
                }

                "openOutputStream", "openAssetFileDescriptor", "openFileDescriptor",
                "openFile", "openAssetFile", "openOutputStreamCompat" -> hookMethod(m) { chain ->
                    if (on(XpConfig.KEY_FILE_OP_WRITE) && isMediaUri(chain)) {
                        logWarn("blocked ContentResolver.${m.name} (write)")
                        null
                    } else {
                        chain.proceed()
                    }
                }

                "openInputStream" -> hookMethod(m) { chain ->
                    if (on(XpConfig.KEY_FILE_OP_READ) && isMediaUri(chain)) {
                        logWarn("blocked ContentResolver.openInputStream (read)")
                        null
                    } else {
                        chain.proceed()
                    }
                }

                "query" -> hookMethod(m) { chain ->
                    if (on(XpConfig.KEY_FILE_OP_QUERY) && isMediaUri(chain)) {
                        logWarn("blocked ContentResolver.query")
                        null
                    } else {
                        chain.proceed()
                    }
                }

                "update" -> hookMethod(m) { chain ->
                    if (on(XpConfig.KEY_FILE_OP_UPDATE) && isMediaUri(chain)) {
                        logWarn("blocked ContentResolver.update")
                        0
                    } else {
                        chain.proceed()
                    }
                }

                "delete" -> hookMethod(m) { chain ->
                    if (on(XpConfig.KEY_FILE_OP_DELETE) && isMediaUri(chain)) {
                        logWarn("blocked ContentResolver.delete")
                        deniedFor(m)
                    } else {
                        chain.proceed()
                    }
                }
            }
        }
    }

    
    private fun isMediaUri(chain: io.github.libxposed.api.XposedInterface.Chain): Boolean {
        val args = runCatching { chain.args }.getOrNull() ?: return false
        val uri = args.firstOrNull { it is Uri } as? Uri ?: return false
        val authority = uri.authority ?: return false
        return authority == "media" || authority.endsWith(".media") ||
                authority.contains("media")
    }

    

    private fun hookJavaFile() {
        val f = runCatching { Class.forName("java.io.File") }.getOrNull() ?: return
        val names = setOf(
            "createNewFile", "mkdir", "mkdirs",
            "delete", "deleteOnExit", "renameTo",
            "createTempFile",
        )
        f.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                if (!on(XpConfig.KEY_FILE_OP_JAVA)) return@hookMethod chain.proceed()
                if (m.name.startsWith("create") || m.name.startsWith("mkdir") ||
                    m.name == "delete" || m.name == "deleteOnExit" || m.name == "renameTo"
                ) {
                    
                    val path = (chain.getThisObject() as? java.io.File)?.absolutePath
                    if (path == null || isPublicPath(path)) {
                        logWarn("blocked java.io.File.${m.name}: $path")
                        return@hookMethod deniedFor(m)
                    }
                }
                chain.proceed()
            }
        }
    }

    

    private fun hookNioFiles() {
        val files = runCatching { Class.forName("java.nio.file.Files") }.getOrNull() ?: return
        val names = setOf(
            "createFile", "createDirectory", "createDirectories", "createTempFile",
            "write", "writeString", "copy", "move", "delete", "deleteIfExists",
            "newOutputStream", "newBufferedWriter",
        )
        files.declaredMethods.filter { it.name in names }.forEach { m ->
            hookMethod(m) { chain ->
                if (!on(XpConfig.KEY_FILE_OP_NIO)) return@hookMethod chain.proceed()
                val path = chain.args.firstOrNull()?.toString()
                if (path == null || isPublicPath(path)) {
                    logWarn("blocked Files.${m.name}: $path")
                    return@hookMethod deniedFor(m)
                }
                chain.proceed()
            }
        }
    }

    private fun isPublicPath(path: String): Boolean =
        PUBLIC_PREFIXES.any { path.startsWith(it) }
}
