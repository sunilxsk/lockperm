package io.github.sunilxsk.lockperm

import android.content.SharedPreferences
import io.github.libxposed.api.XposedModule


internal object FakeFiles {
    @Volatile var uptime: String? = null
    @Volatile var cpuinfo: String? = null
    @Volatile var version: String? = null
    @Volatile var temp: String? = null
    @Volatile var capacity: String? = null
    @Volatile var meminfo: String? = null

    
    private val extras = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun setExtra(path: String, content: String?) {
        if (content == null) extras.remove(path) else extras[path] = content
    }

    fun hasAny(): Boolean =
        uptime != null || cpuinfo != null || version != null || temp != null ||
                capacity != null || meminfo != null || extras.isNotEmpty()

    fun content(path: String): String? {
        extras[path]?.let { return it }
        if (path == "/proc/uptime") return uptime
        if (path == "/proc/cpuinfo") return cpuinfo
        if (path == "/proc/version") return version
        if (path == "/proc/version_signature") return version
        if (path == "/proc/meminfo") return meminfo
        if (path.startsWith("/sys/class/thermal/") && path.endsWith("/temp")) return temp
        if (path == "/sys/class/power_supply/battery/capacity") return capacity
        return null
    }
}












internal class FileSpoofer(
    module: XposedModule,
    prefs: SharedPreferences,
    classLoader: ClassLoader,
) : HookSupport(module, prefs, classLoader) {

    companion object {
        
        val bypass: ThreadLocal<Boolean> = ThreadLocal.withInitial<Boolean> { false }

        fun readRaw(path: String): String? {
            if (bypass.get() == true) return null
            bypass.set(true)
            return try {
                java.io.FileInputStream(path).use { it.readBytes() }
                    .toString(Charsets.UTF_8)
            } catch (_: Throwable) {
                null
            } finally {
                bypass.set(false)
            }
        }
    }

    private val data = java.util.Collections.synchronizedMap(java.util.WeakHashMap<Any, ByteArray>())
    private val pos = java.util.Collections.synchronizedMap(java.util.WeakHashMap<Any, Int>())

    @Volatile
    private var armed = false

    fun install() {
        if (!FakeFiles.hasAny()) return
        armed = true
        hookFileInputStream()
        hookRandomAccessFile()
        hookNioFiles()
        logInfo("file spoofer installed")
    }

    private fun remember(self: Any?, path: String?) {
        if (self == null || path.isNullOrEmpty()) return
        val c = FakeFiles.content(path) ?: return
        data[self] = c.toByteArray(Charsets.UTF_8)
        pos[self] = 0
    }

    private fun pathOf(file: Any?): String? {
        if (file == null) return null
        return runCatching {
            file.javaClass.getMethod("getPath").invoke(file) as? String
        }.getOrNull()
    }

    
    private fun feed(self: Any?, buf: ByteArray?, off: Int, len: Int): Int? {
        if (!armed || self == null || buf == null) return null
        val bytes = data[self] ?: return null
        val p = pos[self] ?: 0
        if (p >= bytes.size) return -1
        val n = minOf(len, bytes.size - p)
        if (n <= 0) return -1
        System.arraycopy(bytes, p, buf, off, n)
        pos[self] = p + n
        return n
    }

    

    private fun hookFileInputStream() {
        val fis = loadClassAnywhere("java.io.FileInputStream") ?: return

        fis.declaredConstructors.forEach { c ->
            val ps = c.parameterTypes
            if (ps.size != 1) return@forEach
            val isString = ps[0].name == "java.lang.String"
            val isFile = ps[0].name == "java.io.File"
            if (!isString && !isFile) return@forEach
            hookCtor(c) { chain ->
                val raw = chain.getArg(0)
                val path = if (isString) raw as? String else pathOf(raw)
                chain.proceed()
                remember(chain.getThisObject(), path)
                null
            }
        }

        runCatching {
            fis.getDeclaredMethod("read", ByteArray::class.java, INT_TYPE, INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val buf = chain.getArg(0) as? ByteArray
                    val off = chain.getArg(1) as? Int ?: 0
                    val len = chain.getArg(2) as? Int ?: 0
                    feed(self, buf, off, len) ?: chain.proceed()
                }
            }
        }
        
        
        
        runCatching {
            fis.getDeclaredMethod("read", ByteArray::class.java).let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val buf = chain.getArg(0) as? ByteArray ?: return@hookMethod chain.proceed()
                    feed(self, buf, 0, buf.size) ?: chain.proceed()
                }
            }
        }
        runCatching {
            fis.getDeclaredMethod("read").let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val one = ByteArray(1)
                    val n = feed(self, one, 0, 1)
                    if (n == null) {
                        chain.proceed()
                    } else if (n <= 0) {
                        -1
                    } else {
                        one[0].toInt() and 0xFF
                    }
                }
            }
        }
        runCatching {
            fis.getDeclaredMethod("available").let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val bytes = data[self] ?: return@hookMethod chain.proceed()
                    val p = pos[self] ?: 0
                    maxOf(0, bytes.size - p)
                }
            }
        }
    }

    

    private fun hookRandomAccessFile() {
        val raf = loadClassAnywhere("java.io.RandomAccessFile") ?: return

        raf.declaredConstructors.forEach { c ->
            val ps = c.parameterTypes
            if (ps.isEmpty()) return@forEach
            val isString = ps[0].name == "java.lang.String"
            val isFile = ps[0].name == "java.io.File"
            if (!isString && !isFile) return@forEach
            hookCtor(c) { chain ->
                val raw = chain.getArg(0)
                val path = if (isString) raw as? String else pathOf(raw)
                chain.proceed()
                remember(chain.getThisObject(), path)
                null
            }
        }

        runCatching {
            raf.getDeclaredMethod("read", ByteArray::class.java, INT_TYPE, INT_TYPE).let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val buf = chain.getArg(0) as? ByteArray
                    val off = chain.getArg(1) as? Int ?: 0
                    val len = chain.getArg(2) as? Int ?: 0
                    feed(self, buf, off, len) ?: chain.proceed()
                }
            }
        }
        runCatching {
            raf.getDeclaredMethod("read", ByteArray::class.java).let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val buf = chain.getArg(0) as? ByteArray ?: return@hookMethod chain.proceed()
                    feed(self, buf, 0, buf.size) ?: chain.proceed()
                }
            }
        }
        runCatching {
            raf.getDeclaredMethod("readLine").let { m ->
                hookMethod(m) { chain ->
                    val self = chain.getThisObject()
                    val bytes = data[self] ?: return@hookMethod chain.proceed()
                    val p = pos[self] ?: 0
                    if (p >= bytes.size) return@hookMethod null
                    val text = String(bytes, Charsets.UTF_8)
                    val rest = text.substring(p)
                    val idx = rest.indexOf('\n')
                    val line = if (idx < 0) rest else rest.substring(0, idx)
                    pos[self] = p + line.toByteArray(Charsets.UTF_8).size + if (idx < 0) 0 else 1
                    line
                }
            }
        }
    }

    

    private fun hookNioFiles() {
        val files = loadClassAnywhere("java.nio.file.Files") ?: return
        val pathCls = runCatching { Class.forName("java.nio.file.Path") }.getOrNull() ?: return

        runCatching {
            files.getDeclaredMethod("readAllBytes", pathCls).let { m ->
                hookMethod(m) { chain ->
                    val c = chain.getArg(0)?.toString()?.let { FakeFiles.content(it) }
                    if (c == null) chain.proceed() else c.toByteArray(Charsets.UTF_8)
                }
            }
        }
        runCatching {
            files.getDeclaredMethod("readAllLines", pathCls).let { m ->
                hookMethod(m) { chain ->
                    val c = chain.getArg(0)?.toString()?.let { FakeFiles.content(it) }
                    if (c == null) chain.proceed() else c.split("\n")
                }
            }
        }
        runCatching {
            files.getDeclaredMethod("readString", pathCls).let { m ->
                hookMethod(m) { chain ->
                    val c = chain.getArg(0)?.toString()?.let { FakeFiles.content(it) }
                    if (c == null) chain.proceed() else c
                }
            }
        }
    }
}
