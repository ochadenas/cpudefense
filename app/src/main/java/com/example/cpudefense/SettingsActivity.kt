package com.example.cpudefense

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat


class SettingsActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        loadPrefs()
    }

    fun loadPrefs()
    {
        val disableBackgroundView = findViewById<SwitchCompat>(R.id.switch_disable_background)
        val showRangeView = findViewById<SwitchCompat>(R.id.switch_show_atts_in_range)
        val useLargeButtons = findViewById<SwitchCompat>(R.id.switch_use_large_buttons)
        val showFrameRate = findViewById<SwitchCompat>(R.id.switch_show_framerate)
        val fastFastForward = findViewById<SwitchCompat>(R.id.switch_fast_fast_forward)
        val keepLevels = findViewById<SwitchCompat>(R.id.switch_keep_levels)
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        disableBackgroundView.isChecked = prefs.getBoolean("DISABLE_BACKGROUND", false)
        showRangeView.isChecked = prefs.getBoolean("SHOW_ATTS_IN_RANGE", false)
        useLargeButtons.isChecked = prefs.getBoolean("USE_LARGE_BUTTONS", false)
        showFrameRate.isChecked = prefs.getBoolean("SHOW_FRAMERATE", false)
        fastFastForward.isChecked = prefs.getBoolean("USE_FAST_FAST_FORWARD", false)
        keepLevels.isChecked = prefs.getBoolean("KEEP_LEVELS", true)
    }

    fun savePrefs(v: View)
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("DISABLE_BACKGROUND", findViewById<SwitchCompat>(R.id.switch_disable_background)?.isChecked ?: false)
            putBoolean("SHOW_ATTS_IN_RANGE", findViewById<SwitchCompat>(R.id.switch_show_atts_in_range)?.isChecked ?: false)
            putBoolean("USE_LARGE_BUTTONS", findViewById<SwitchCompat>(R.id.switch_use_large_buttons)?.isChecked ?: false)
            putBoolean("SHOW_FRAMERATE", findViewById<SwitchCompat>(R.id.switch_show_framerate)?.isChecked ?: false)
            putBoolean("USE_FAST_FAST_FORWARD", findViewById<SwitchCompat>(R.id.switch_fast_fast_forward)?.isChecked ?: false)
            putBoolean("KEEP_LEVELS", findViewById<SwitchCompat>(R.id.switch_keep_levels)?.isChecked ?: true)
            commit()
        }
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }

    fun about(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

}