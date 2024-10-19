package com.example.cpudefense

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.util.prefs.Preferences


class SettingsActivity : AppCompatActivity()
{
    var settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        loadPrefs()
    }

    private fun loadPrefs()
    {
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.loadFromFile(prefs)
        findViewById<SwitchCompat>(R.id.switch_disable_purchase_dialog)?.isChecked = settings.configDisablePurchaseDialog
        findViewById<SwitchCompat>(R.id.switch_disable_background)?.isChecked = settings.configDisableBackground
        findViewById<SwitchCompat>(R.id.switch_show_atts_in_range)?.isChecked = settings.configShowAttackersInRange
        findViewById<SwitchCompat>(R.id.switch_use_large_buttons)?.isChecked = settings.configUseLargeButtons
        findViewById<SwitchCompat>(R.id.switch_show_framerate)?.isChecked = settings.showFrameRate
        findViewById<SwitchCompat>(R.id.switch_fast_fast_forward)?.isChecked = settings.fastFastForward
        findViewById<SwitchCompat>(R.id.switch_keep_levels)?.isChecked = settings.keepLevels
    }

    @Suppress("UNUSED_PARAMETER")
    fun savePrefs(v: View)
    {
        settings.configDisablePurchaseDialog = findViewById<SwitchCompat>(R.id.switch_disable_purchase_dialog)?.isChecked ?: false
        settings.configDisableBackground = findViewById<SwitchCompat>(R.id.switch_disable_background)?.isChecked ?: false
        settings.configShowAttackersInRange = findViewById<SwitchCompat>(R.id.switch_show_atts_in_range)?.isChecked ?: false
        settings.configUseLargeButtons = findViewById<SwitchCompat>(R.id.switch_use_large_buttons)?.isChecked ?: false
        settings.showFrameRate = findViewById<SwitchCompat>(R.id.switch_show_framerate)?.isChecked ?: false
        settings.fastFastForward = findViewById<SwitchCompat>(R.id.switch_fast_fast_forward)?.isChecked ?: false
        settings.keepLevels = findViewById<SwitchCompat>(R.id.switch_keep_levels)?.isChecked ?: true
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.saveToFile(prefs)
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }

    fun startNewGame(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val dialog = Dialog(this)
        // dialog.setContentView(R.layout.layout_dialog_new_game)
        dialog.setContentView(R.layout.layout_dialog_new_game)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.question).text = resources.getText(R.string.query_restart_game)
        val button1 = dialog.findViewById<Button>(R.id.button1)
        val button2 = dialog.findViewById<Button>(R.id.button2)
        button2?.text = resources.getText(R.string.yes)
        button1?.text = resources.getText(R.string.no)
        button2?.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("RESET_PROGRESS", true)
            intent.putExtra("START_ON_STAGE", 1)
            intent.putExtra("CONTINUE_GAME", false)
            startActivity(intent)
        }
        button1?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

}