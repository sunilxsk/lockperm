package io.github.sunilxsk.lockperm

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
















object UiNav {
    
    var jumpToLocation by mutableStateOf(0)
        private set

    fun requestLocation() {
        jumpToLocation++
    }
}

object UiSettings {

    
    var themeMode by mutableStateOf(XpConfig.THEME_DEFAULT)
        private set

    
    var themeColor by mutableStateOf(XpConfig.DEF_THEME_COLOR)
        private set

    
    var appIcon by mutableStateOf(0)
        private set

    
    var uiScale by mutableStateOf(XpConfig.DEF_UI_SCALE / 100f)
        private set

    
    var templateColumns by mutableStateOf(XpConfig.DEF_TEMPLATE_COLUMNS)
        private set

    
    var autoUpdate by mutableStateOf(false)
        private set

    




    var lspatchActivate by mutableStateOf(false)
        private set

    
    var lspatchScopeNoticed by mutableStateOf<Set<String>>(emptySet())
        private set

    
    var hideLauncher by mutableStateOf(false)
        private set

    
    var predictiveBack by mutableStateOf(false)
        private set

    private var ready = false

    fun load(context: Context) {
        val sp = context.getSharedPreferences(XpConfig.UI_PREFS, Context.MODE_PRIVATE)
        themeMode = sp.getInt(XpConfig.UI_THEME_MODE, XpConfig.THEME_DEFAULT)
        themeColor = sp.getInt(XpConfig.UI_THEME_COLOR, XpConfig.DEF_THEME_COLOR)
        appIcon = sp.getInt(XpConfig.UI_APP_ICON, 0)
            .coerceIn(0, XpConfig.APP_ICON_ALIASES.lastIndex)
        uiScale = sp.getInt(XpConfig.UI_SCALE, XpConfig.DEF_UI_SCALE) / 100f
        templateColumns = sp.getInt(XpConfig.UI_TEMPLATE_COLUMNS, XpConfig.DEF_TEMPLATE_COLUMNS)
            .coerceIn(1, XpConfig.MAX_TEMPLATE_COLUMNS)
        autoUpdate = sp.getBoolean(XpConfig.UI_AUTO_UPDATE, false)
        lspatchActivate = sp.getBoolean(XpConfig.UI_LSPATCH_ACTIVATE, false)
        lspatchScopeNoticed = XpConfig.decodeSet(
            sp.getString(XpConfig.UI_LSPATCH_SCOPE_NOTICED, "") ?: ""
        )
        hideLauncher = sp.getBoolean(XpConfig.UI_HIDE_LAUNCHER, false)
        predictiveBack = sp.getBoolean(XpConfig.UI_PREDICTIVE_BACK, false)
        if (!ready) {
            
            if (hideLauncher) applyHideLauncher(context, true)
            else applyIcon(context, appIcon)
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

    fun setTemplateColumns(context: Context, columns: Int) {
        val c = columns.coerceIn(1, XpConfig.MAX_TEMPLATE_COLUMNS)
        templateColumns = c
        edit(context) { putInt(XpConfig.UI_TEMPLATE_COLUMNS, c) }
    }

    fun setAutoUpdate(context: Context, on: Boolean) {
        autoUpdate = on
        edit(context) { putBoolean(XpConfig.UI_AUTO_UPDATE, on) }
    }

    fun setLspatchActivate(context: Context, on: Boolean) {
        lspatchActivate = on
        edit(context) { putBoolean(XpConfig.UI_LSPATCH_ACTIVATE, on) }
    }

    
    fun markLspatchScopeNoticed(context: Context, pkg: String) {
        if (pkg.isBlank() || pkg in lspatchScopeNoticed) return
        val next = lspatchScopeNoticed + pkg
        lspatchScopeNoticed = next
        edit(context) {
            putString(XpConfig.UI_LSPATCH_SCOPE_NOTICED, XpConfig.encodeSet(next))
        }
    }

    



    fun setAppIcon(context: Context, index: Int) {
        val i = index.coerceIn(0, XpConfig.APP_ICON_ALIASES.lastIndex)
        appIcon = i
        edit(context) { putInt(XpConfig.UI_APP_ICON, i) }
        applyIcon(context, i)
    }

    fun setHideLauncher(context: Context, on: Boolean) {
        hideLauncher = on
        edit(context) { putBoolean(XpConfig.UI_HIDE_LAUNCHER, on) }
        applyHideLauncher(context, on)
        if (!on) {
            
            
            
            applyIcon(context, appIcon)
        }
    }

    fun setPredictiveBack(context: Context, on: Boolean) {
        predictiveBack = on
        edit(context) { putBoolean(XpConfig.UI_PREDICTIVE_BACK, on) }
    }

    









    private fun applyHideLauncher(context: Context, on: Boolean) {
        val pm = context.packageManager
        XpConfig.APP_ICON_ALIASES.forEach { name ->
            val cn = ComponentName(context, name)
            runCatching {
                pm.setComponentEnabledSetting(
                    cn,
                    if (on) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    else PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
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
