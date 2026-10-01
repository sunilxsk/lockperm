package io.github.sunilxsk.lockperm

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue








object UiSettings {

    
    var themeMode by mutableStateOf(XpConfig.THEME_DEFAULT)
        private set

    
    var themeColor by mutableStateOf(XpConfig.DEF_THEME_COLOR)
        private set

    
    var appIcon by mutableStateOf(0)
        private set

    
    var uiScale by mutableStateOf(XpConfig.DEF_UI_SCALE / 100f)
        private set

    private var ready = false

    fun load(context: Context) {
        val sp = context.getSharedPreferences(XpConfig.UI_PREFS, Context.MODE_PRIVATE)
        themeMode = sp.getInt(XpConfig.UI_THEME_MODE, XpConfig.THEME_DEFAULT)
        themeColor = sp.getInt(XpConfig.UI_THEME_COLOR, XpConfig.DEF_THEME_COLOR)
        appIcon = sp.getInt(XpConfig.UI_APP_ICON, 0)
            .coerceIn(0, XpConfig.APP_ICON_ALIASES.lastIndex)
        uiScale = sp.getInt(XpConfig.UI_SCALE, XpConfig.DEF_UI_SCALE) / 100f
        if (!ready) {
            applyIcon(context, appIcon)
            ready = true
        }
    }

    private fun edit(context: Context, block: android.content.SharedPreferences.Editor.() -> Unit) {
        val sp = context.getSharedPreferences(XpConfig.UI_PREFS, Context.MODE_PRIVATE)
        val ed = sp.edit()
        ed.block()
        ed.apply()
    }

    fun setThemeMode(context: Context, mode: Int) {
        themeMode = mode
        edit(context) { putInt(XpConfig.UI_THEME_MODE, mode) }
    }

    fun setThemeColor(context: Context, color: Int) {
        themeColor = color
        edit(context) { putInt(XpConfig.UI_THEME_COLOR, color) }
    }

    fun setUiScale(context: Context, scale: Float) {
        uiScale = scale
        edit(context) { putInt(XpConfig.UI_SCALE, (scale * 100).toInt()) }
    }

    



    fun setAppIcon(context: Context, index: Int) {
        val i = index.coerceIn(0, XpConfig.APP_ICON_ALIASES.lastIndex)
        appIcon = i
        edit(context) { putInt(XpConfig.UI_APP_ICON, i) }
        applyIcon(context, i)
    }

    private fun applyIcon(context: Context, index: Int) {
        val pm = context.packageManager
        XpConfig.APP_ICON_ALIASES.forEachIndexed { i, name ->
            val cn = ComponentName(context, name)
            val state = if (i == index) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                val cur = pm.getComponentEnabledSetting(cn)
                if (cur != state) {
                    pm.setComponentEnabledSetting(cn, state, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }
}
