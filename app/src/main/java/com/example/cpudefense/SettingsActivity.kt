package com.example.cpudefense

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class SettingsActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<TextView>(R.id.about_text_view)?.movementMethod = ScrollingMovementMethod()
        loadPrefs()
    }

    fun loadPrefs()
    {
        val disableBackgroundView = findViewById<Switch>(R.id.switch_disable_background)
        val showRangeView = findViewById<Switch>(R.id.switch_show_atts_in_range)
        val useLargeButtons = findViewById<Switch>(R.id.switch_use_large_buttons)
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        disableBackgroundView.isChecked = prefs.getBoolean("DISABLE_BACKGROUND", false)
        showRangeView.isChecked = prefs.getBoolean("SHOW_ATTS_IN_RANGE", false)
        useLargeButtons.isChecked = prefs.getBoolean("USE_LARGE_BUTTONS", false)
    }

    fun savePrefs(v: View)
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("DISABLE_BACKGROUND", findViewById<Switch>(R.id.switch_disable_background)?.isChecked ?: false)
            putBoolean("SHOW_ATTS_IN_RANGE", findViewById<Switch>(R.id.switch_show_atts_in_range)?.isChecked ?: false)
            putBoolean("USE_LARGE_BUTTONS", findViewById<Switch>(R.id.switch_use_large_buttons)?.isChecked ?: false)
            commit()
        }
    }


}