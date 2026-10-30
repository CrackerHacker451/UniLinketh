package com.example.unilink.utils

import android.app.Activity
import android.content.Context
import com.example.unilink.R

object ThemeUtils {
    private const val PREFS_NAME = "unilink_theme_prefs"
    private const val KEY_THEME = "selected_theme"

    fun applyTheme(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val theme = prefs.getString(KEY_THEME, "Cyber")
        
        val themeResId = when (theme) {
            "Indigo Liquid" -> R.style.Theme_UniLink_Indigo
            "Ocean Glass" -> R.style.Theme_UniLink_Teal
            "Rose Crystal" -> R.style.Theme_UniLink_Rose
            "Amber Glow" -> R.style.Theme_UniLink_Amber
            "WhatsApp Light" -> R.style.Theme_UniLink_Light
            else -> R.style.Theme_UniLink // Cyber Default
        }
        activity.setTheme(themeResId)
    }

    fun saveTheme(context: Context, themeName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, themeName)
            .apply()
    }
    
    fun getSelectedTheme(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "Cyber") ?: "Cyber"
    }
}
