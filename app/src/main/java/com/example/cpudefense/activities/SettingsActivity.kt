package com.example.cpudefense.activities

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.Settings
import com.example.cpudefense.databinding.ActivitySettingsBinding
import com.example.cpudefense.databinding.LayoutDialogExportgameBinding
import com.example.cpudefense.databinding.LayoutDialogResetProgressBinding
import com.example.cpudefense.utils.TimeStamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class SettingsActivity : AppCompatActivity() {
    private var settings = Settings()
    private var isEndlessAvailable = false
    private var createExportLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(), ::doExport)
    private var createImportLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument(), ::parseImportFile)
    var packageInfo: PackageInfo? = null

    private lateinit var settingsBinding: ActivitySettingsBinding
    private lateinit var layoutDialogExportgameBinding: LayoutDialogExportgameBinding
    private lateinit var layoutDialogResetProgressBinding: LayoutDialogResetProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // supportRequestWindowFeature(Window.FEATURE_NO_TITLE)  // method of AppCompatActivity
        WindowCompat.enableEdgeToEdge(window)
        if (intent.getIntExtra("MAXSERIES", 1) >= GameMechanics.SERIES_ENDLESS)
            isEndlessAvailable = true
        settingsBinding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = settingsBinding.root
        setContentView(view)
        findViewById<View>(android.R.id.content).let {
            ViewCompat.setOnApplyWindowInsetsListener(it, ::handleInsets)
        }
        packageInfo = packageManager?.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        loadPrefs()
    }
    fun handleInsets(view: View, windowInsets: WindowInsetsCompat): WindowInsetsCompat
            /** handles the width of the system status bar (top and bottom) and applies
             * margins in order to avoid overlapping of game elements
             */
    {
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        settingsBinding.actionsTitleView
            .updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = insets.top }
        return WindowInsetsCompat.CONSUMED
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.loadFromFile(prefs)
        settingsBinding.switchDisablePurchaseDialog.isChecked = settings.configDisablePurchaseDialog
        settingsBinding.switchDisableBackground.isChecked = settings.configDisableBackground
        settingsBinding.switchShowAttsInRange.isChecked = settings.configShowAttackersInRange
        settingsBinding.switchUseLargeButtons.isChecked = settings.configUseLargeButtons
        settingsBinding.switchShowFramerate.isChecked = settings.showFrameRate
        settingsBinding.switchFastFastForward.isChecked = settings.fastFastForward
        settingsBinding.switchZoom.isChecked = settings.zoom
        settingsBinding.switchKeepLevels.isChecked = settings.keepLevels
        settingsBinding.switchUseHex.isChecked = settings.showLevelsInHex
        settingsBinding.switchActivateLog.let {
            it.isChecked = settings.activateLogging
            if (GameMechanics.enableLogging) {
                it.visibility = VISIBLE
                it.isEnabled = true
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun savePrefs(v: View) {
        settings.configDisablePurchaseDialog = settingsBinding.switchDisablePurchaseDialog.isChecked
        settings.configDisableBackground = settingsBinding.switchDisableBackground.isChecked
        settings.configShowAttackersInRange = settingsBinding.switchShowAttsInRange.isChecked
        settings.configUseLargeButtons = settingsBinding.switchUseLargeButtons.isChecked
        settings.showFrameRate = settingsBinding.switchShowFramerate.isChecked
        settings.fastFastForward = settingsBinding.switchFastFastForward.isChecked
        settings.zoom = settingsBinding.switchZoom.isChecked
        settings.keepLevels = settingsBinding.switchKeepLevels.isChecked
        settings.showLevelsInHex = settingsBinding.switchUseHex.isChecked
        settings.activateLogging = settingsBinding.switchActivateLog.isChecked
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.saveToFile(prefs)
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View) {
        finish()
    }

    fun startNewGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val dialog = Dialog(this)
        layoutDialogResetProgressBinding = LayoutDialogResetProgressBinding.inflate(layoutInflater)
        dialog.setContentView(layoutDialogResetProgressBinding.root)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        layoutDialogResetProgressBinding.question.text =
            resources.getText(R.string.query_restart_game)
        layoutDialogResetProgressBinding.button1.let {
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
        layoutDialogResetProgressBinding.optionCancel.let {
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
        layoutDialogResetProgressBinding.button3.let {
            it.text = resources.getText(R.string.choice_3)
            it.setOnClickListener {
                dialog.dismiss()
            }
        }
        dialog.show()
    }


    fun importGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val dialog = Dialog(this)
        layoutDialogExportgameBinding = LayoutDialogExportgameBinding.inflate(layoutInflater)
        dialog.setContentView(layoutDialogExportgameBinding.root)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        layoutDialogExportgameBinding.question.text = getString(R.string.text_importgame_question)
        layoutDialogExportgameBinding.optionCancel.let {
            it.text = getString(R.string.button_cancel)
            it.setOnClickListener { dialog.dismiss() }
        }
        layoutDialogExportgameBinding.button1.let {
            it.text = getString(R.string.text_importgame_option1)
            it.setOnClickListener {
                createImportLauncher.launch(arrayOf("text/json", "application/json", "text/plain", "*/*"))
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun parseImportFile(uri: Uri?) {
        uri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val persistency = Persistency(this)
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    if (jsonString.isEmpty() or jsonString.isBlank())
                    {
                        Toast.makeText(this, getString(R.string.error_empty_file), Toast.LENGTH_LONG).show()
                        return
                    }
                    val infoString = buildInfoString(persistency.parseGameImport(jsonString))

                    val dialog = Dialog(this)
                    layoutDialogExportgameBinding = LayoutDialogExportgameBinding.inflate(layoutInflater)
                    dialog.setContentView(layoutDialogExportgameBinding.root)
                    dialog.window?.setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    dialog.setCancelable(true)
                    layoutDialogExportgameBinding.question.text = infoString
                    layoutDialogExportgameBinding.button1.let {
                        it.text = resources.getText(R.string.import_choice_yes)
                        it.setOnClickListener {
                            // pass the file name to the game activity
                            getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE).edit {
                                putString("IMPORTFILE", uri.toString())
                            }
                            val intent = Intent(this, GameActivity::class.java)
                            intent.putExtra("LOAD_PROGRESS", true)
                            intent.putExtra("CONTINUE_GAME", false)
                            startActivity(intent)
                            Toast.makeText(this, getString(R.string.toast_loading), Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                            finish()
                        }
                    }
                    dialog.findViewById<TextView>(R.id.option_cancel)?.let {
                        it.text = resources.getText(R.string.import_choice_no)
                        it.setOnClickListener { dialog.dismiss() }
                    }
                    dialog.show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, resources.getString(R.string.error_reading_file).format(e.message), Toast.LENGTH_LONG).show()
                displayError(e.message)
            }
        }
        return
    }

    fun buildInfoString(gameInfo: Persistency.SaveFileInfo?): String
    {
        // get version info
        var versionInfoString = getString(R.string.warning_unknown_version)
        gameInfo?.let { info -> info.gameVersion.let  { version ->
            if (version != packageInfo?.versionName)
                versionInfoString = getString(R.string.warning_version_mismatch).format(version, packageInfo?.versionName ?: getString(R.string.three_question_marks))
            else {
                versionInfoString = getString(R.string.message_version).format(info.gameVersion)
            }
        }
        }
        // extract export date and time
        var timeInfoString = ""
        try { gameInfo?.let {
            val timestamp = TimeStamp(resources, it.exportDate).apply { convertToReadable() }
            timeInfoString = getString(R.string.message_timestamp).format(timestamp.readableDate, timestamp.readableTime)
        }}
        catch (_: Exception ) { timeInfoString = getString(R.string.warning_no_timestamp) }

        // extract info about max level
        var maxProgressString = ""
        gameInfo?.let { info ->
            if (info.maxSeries>0)
                maxProgressString = getString(R.string.message_progress).format(info.maxStage, info.maxSeries)
        }
        // assemble the info string
        return getString(R.string.question_import_confirmation).format(versionInfoString, timeInfoString, maxProgressString)
    }

    fun displayError(message: String?) {
        val dialog = Dialog(this)
        layoutDialogExportgameBinding = LayoutDialogExportgameBinding.inflate(layoutInflater)
        dialog.setContentView(layoutDialogExportgameBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT )
        dialog.setCancelable(true)
        layoutDialogExportgameBinding.question.text =
            getString(R.string.error_reading_file).format(message ?: "")
        layoutDialogExportgameBinding.optionCancel.let {
            it.text = getString(R.string.shithappens_1)
            it.setOnClickListener { dialog.dismiss() }
        }
        layoutDialogExportgameBinding.button1.let {
            it.text = getString(R.string.shithappens_2)
            it.setOnClickListener { dialog.dismiss() }
        }
        dialog.show()
    }

    fun exportGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val fileName = getString(R.string.default_export_filename).format(currentDate)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_exportgame)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.findViewById<TextView>(R.id.question)?.let {
            it.text = getString(R.string.text_exportgame_question, fileName)
        }
        dialog.findViewById<TextView>(R.id.option_cancel)?.let {
            it.setOnClickListener { dialog.dismiss() }
        }
        dialog.findViewById<TextView>(R.id.button1)?.let {
            it.setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "text/json"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                createExportLauncher.launch(intent)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun doExport(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileContent = Persistency(this).prepareGameExport()
                try {
                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(fileContent.toByteArray())
                    }
                    Toast.makeText(this, getString(R.string.message_export_successful), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.message_export_error, e.message), Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }
}