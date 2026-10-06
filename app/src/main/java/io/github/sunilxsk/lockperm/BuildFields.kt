package io.github.sunilxsk.lockperm

import java.lang.reflect.Field














internal object BuildFields {

    fun apply(cfg: XpState.Snapshot) {
        if (!cfg.enableBuild) return

        val values = LinkedHashMap<String, String>(cfg.buildValues)
        if (values.isEmpty()) return

        val build = runCatching { Class.forName("android.os.Build") }.getOrNull() ?: return
        val version = runCatching { Class.forName("android.os.Build\$VERSION") }.getOrNull()

        var ok = 0
        values.forEach { (field, value) ->
            val (cls, name) = if (field.startsWith("VERSION.")) {
                val n = field.removePrefix("VERSION.")
                if (version == null) return@forEach
                version to n
            } else {
                build to field
            }
            if (set(cls, name, value)) ok++
        }
        if (XpState.Flags.logEnabled) {
            android.util.Log.i("LockPerm", "BuildFields: applied $ok/${values.size}")
        }
    }

    private fun set(clazz: Class<*>, name: String, value: String): Boolean {
        return runCatching {
            val f: Field = clazz.getDeclaredField(name)
            f.isAccessible = true
            clearFinal(f)
            
            when (f.type) {
                java.lang.Integer.TYPE, Int::class.javaObjectType ->
                    f.setInt(null, value.toIntOrNull() ?: return@runCatching false)
                else -> f.set(null, value)
            }
            true
        }.getOrDefault(false)
    }

    private fun clearFinal(f: Field) {
        runCatching {
            val m = Field::class.java.getDeclaredField("modifiers")
            m.isAccessible = true
            m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        }.onFailure {
            runCatching {
                val m = Field::class.java.getDeclaredField("accessFlags")
                m.isAccessible = true
                m.setInt(f, f.modifiers and java.lang.reflect.Modifier.FINAL.inv())
            }
        }
    }
}
