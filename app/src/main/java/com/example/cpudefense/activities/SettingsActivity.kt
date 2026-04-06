package com.example.cpudefense.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.text.format


class SettingsActivity : AppCompatActivity() {
    var settings = Settings()
    private var isEndlessAvailable = false

    // Vorbereitung für den Export des Spielstands
    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::doExport)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)  // method of AppCompatActivity
        if (intent.getIntExtra("MAXSERIES", 1) >= GameMechanics.SERIES_ENDLESS)
            isEndlessAvailable = true
        setContentView(R.layout.activity_settings)
        loadPrefs()
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.loadFromFile(prefs)
        findViewById<SwitchCompat>(R.id.switch_disable_purchase_dialog)?.isChecked =
            settings.configDisablePurchaseDialog
        findViewById<SwitchCompat>(R.id.switch_disable_background)?.isChecked =
            settings.configDisableBackground
        findViewById<SwitchCompat>(R.id.switch_show_atts_in_range)?.isChecked =
            settings.configShowAttackersInRange
        findViewById<SwitchCompat>(R.id.switch_use_large_buttons)?.isChecked =
            settings.configUseLargeButtons
        findViewById<SwitchCompat>(R.id.switch_show_framerate)?.isChecked = settings.showFrameRate
        findViewById<SwitchCompat>(R.id.switch_fast_fast_forward)?.isChecked =
            settings.fastFastForward
        findViewById<SwitchCompat>(R.id.switch_keep_levels)?.isChecked = settings.keepLevels
        findViewById<SwitchCompat>(R.id.switch_use_hex)?.isChecked = settings.showLevelsInHex
        findViewById<SwitchCompat>(R.id.switch_activate_log)?.let {
            it.isChecked = settings.activateLogging
            if (GameMechanics.enableLogging) {
                it.visibility = VISIBLE
                it.isEnabled = true
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun savePrefs(v: View) {
        settings.configDisablePurchaseDialog =
            findViewById<SwitchCompat>(R.id.switch_disable_purchase_dialog)?.isChecked ?: false
        settings.configDisableBackground =
            findViewById<SwitchCompat>(R.id.switch_disable_background)?.isChecked ?: false
        settings.configShowAttackersInRange =
            findViewById<SwitchCompat>(R.id.switch_show_atts_in_range)?.isChecked ?: false
        settings.configUseLargeButtons =
            findViewById<SwitchCompat>(R.id.switch_use_large_buttons)?.isChecked ?: false
        settings.showFrameRate =
            findViewById<SwitchCompat>(R.id.switch_show_framerate)?.isChecked ?: false
        settings.fastFastForward =
            findViewById<SwitchCompat>(R.id.switch_fast_fast_forward)?.isChecked ?: false
        settings.keepLevels = findViewById<SwitchCompat>(R.id.switch_keep_levels)?.isChecked ?: true
        settings.showLevelsInHex =
            findViewById<SwitchCompat>(R.id.switch_use_hex)?.isChecked ?: false
        settings.activateLogging =
            findViewById<SwitchCompat>(R.id.switch_activate_log)?.isChecked ?: false
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.saveToFile(prefs)
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View) {
        finish()
    }

    fun startNewGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_reset_progress)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.question).text =
            resources.getText(R.string.query_restart_game)
        dialog.findViewById<TextView>(R.id.button1)?.let {
            it.text = resources.getText(R.string.choice_1)
            it.setOnClickListener {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("RESET_PROGRESS", true)
                intent.putExtra("CONTINUE_GAME", false)
                startActivity(intent)
                dialog.dismiss()
                dismiss(v)
            }
        }
        dialog.findViewById<TextView>(R.id.button2)?.let {
            it.text = resources.getText(R.string.choice_2)
            if (isEndlessAvailable) {
                it.setOnClickListener {
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("RESET_ENDLESS", true)
                    intent.putExtra("CONTINUE_GAME", false)
                    startActivity(intent)
                    dialog.dismiss()
                    dismiss(v)
                }
            } else {
                it.setTextColor(Color.BLACK)
            }
        }
        dialog.findViewById<TextView>(R.id.button3)?.let {
            it.text = resources.getText(R.string.choice_3)
            it.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }


    fun restoreGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_reset_progress)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.question).text =
            resources.getText(R.string.query_restore_game)
        dialog.findViewById<TextView>(R.id.button1)?.let {
            it.text = resources.getText(R.string.choice_restore_1)
            it.setOnClickListener {
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("RESET_PROGRESS", true)
                intent.putExtra("CONTINUE_GAME", false)
                startActivity(intent)
                dialog.dismiss()
                dismiss(v)
            }
        }
        dialog.findViewById<TextView>(R.id.button2)?.let {
            it.setTextColor(Color.BLACK)
        }
        dialog.findViewById<TextView>(R.id.button3)?.let {
            it.text = resources.getText(R.string.choice_restore_2)
            it.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun exportGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val fileName = "chipdefense_export_$currentDate.xml"
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_exportgame)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.button_cancel)?.let {
            it.text = resources.getText(R.string.choice_1)
            it.setOnClickListener {
                dialog.dismiss()
                dismiss(v)
            }
        }
        dialog.findViewById<TextView>(R.id.button_selectpath)?.let {
            it.text = resources.getText(R.string.choice_2)
            it.setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/xml"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                createFileLauncher.launch(intent)
                dialog.dismiss()
                dismiss(v)
            }
        }
        dialog.show()
    }

    private fun doExport(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // 2. XML-Inhalt als String vorbereiten
                val xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <game>
                    <name>Chip Defense</name>
                    <level>5</level>
                </game>
            """.trimIndent()
                // 3. Datei beschreiben
                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(xmlContent.toByteArray())
                    }
                    Toast.makeText(this, "Export erfolgreich!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Fehler beim Export: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}