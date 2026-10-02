package io.github.sunilxsk.lockperm

import java.lang.reflect.Field


















internal object BuildFields {

    
    private val PROP_TO_FIELD: List<Pair<String, String>> = listOf(
        "ro.product.device" to "DEVICE",
        "ro.product.model" to "MODEL",
        "ro.product.brand" to "BRAND",
        "ro.product.manufacturer" to "MANUFACTURER",
        "ro.product.board" to "BOARD",
        "ro.product.name" to "PRODUCT",
        "ro.build.id" to "ID",
        "ro.build.display.id" to "DISPLAY",
        "ro.build.version.incremental" to "VERSION.INCREMENTAL",
        "ro.build.version.release" to "VERSION.RELEASE",
        "ro.build.version.sdk" to "VERSION.SDK",
        "ro.build.type" to "TYPE",
        "ro.build.tags" to "TAGS",
        "ro.build.user" to "USER",
        "ro.build.host" to "HOST",
        "ro.build.fingerprint" to "FINGERPRINT",
        "ro.build.version.security_patch" to "VERSION.SECURITY_PATCH",
        "ro.bootloader" to "BOOTLOADER",
        "ro.hardware" to "HARDWARE",
        "ro.build.product" to "PRODUCT",
    )

    fun apply(cfg: XpState.Snapshot) {
        if (!cfg.enableBuild) return

        
        val values = LinkedHashMap<String, String>(cfg.buildValues)

        
        cfg.customProps.forEach { (k, v) ->
            if (v.isEmpty()) return@forEach
            val hit = PROP_TO_FIELD.firstOrNull { it.first == k } ?: return@forEach
            val field = hit.second
            if (values.containsKey(field)) return@forEach
            if (field.startsWith("VERSION.")) return@forEach  
            values[field] = v
        }

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
                java.lang.Integer.TYPE, Integer::class.java ->
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
