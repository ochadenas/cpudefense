package com.example.cpudefense

import android.content.SharedPreferences

class Settings {
    var configDisablePurchaseDialog: Boolean = false
    var configDisableBackground: Boolean = true
    var configShowAttackersInRange: Boolean = false
    var configUseLargeButtons: Boolean = false
    var showFrameRate: Boolean = false
    var fastFastForward: Boolean = false
    var keepLevels: Boolean = true
    var activateLogging: Boolean = false

    fun loadFromFile(prefs: SharedPreferences): Boolean
    {
        configDisableBackground = prefs.getBoolean("DISABLE_PURCHASE_DIALOG", false)
        configDisableBackground = prefs.getBoolean("DISABLE_BACKGROUND", false)
        configShowAttackersInRange = prefs.getBoolean("SHOW_ATTS_IN_RANGE", false)
        configUseLargeButtons = prefs.getBoolean("USE_LARGE_BUTTONS", false)
        showFrameRate = prefs.getBoolean("SHOW_FRAMERATE", false)
        fastFastForward = prefs.getBoolean("USE_FAST_FAST_FORWARD", false)
        keepLevels = prefs.getBoolean("KEEP_LEVELS", true)
        activateLogging = prefs.getBoolean("LOGGING_ACTIVE", false)
        return prefs.getBoolean("USE_PREFS_FILE", false) // marker whether this file is initialized and in use
    }

    fun saveToFile(prefs: SharedPreferences)
    {
        prefs.edit().apply {
            putBoolean("DISABLE_PURCHASE_DIALOG", configDisablePurchaseDialog)
            putBoolean("DISABLE_BACKGROUND", configDisableBackground)
            putBoolean("SHOW_ATTS_IN_RANGE", configShowAttackersInRange)
            putBoolean("USE_LARGE_BUTTONS", configUseLargeButtons)
            putBoolean("SHOW_FRAMERATE", showFrameRate)
            putBoolean("USE_FAST_FAST_FORWARD", fastFastForward)
            putBoolean("KEEP_LEVELS", keepLevels)
            putBoolean("LOGGING_ACTIVE", activateLogging)
            putBoolean("USE_PREFS_FILE", true)
            apply()
        }
    }

    fun migrateSettings(oldPrefs: SharedPreferences, newPrefs: SharedPreferences)
    {
        if (loadFromFile(newPrefs))
            return // already migrated
        loadFromFile(oldPrefs)
        saveToFile(newPrefs)
        // remove old keys
        oldPrefs.edit().apply {
            remove("DISABLE_PURCHASE_DIALOG")
            remove("DISABLE_BACKGROUND")
            remove("SHOW_ATTS_IN_RANGE")
            remove("USE_LARGE_BUTTONS")
            remove("SHOW_FRAMERATE")
            remove("USE_FAST_FAST_FORWARD")
            remove("KEEP_LEVELS")
            remove("USE_PREFS_FILE")
            apply()
        }
    }

}
