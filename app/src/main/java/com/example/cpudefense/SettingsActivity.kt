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
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        disableBackgroundView.isChecked = prefs.getBoolean("DISABLE_BACKGROUND", false)
    }

    fun savePrefs(v: View)
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("DISABLE_BACKGROUND", findViewById<Switch>(R.id.switch_disable_background)?.isChecked ?: false)
            apply()
        }
    }


}