@file:Suppress("DEPRECATION")

package com.example.cpudefense.activities

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.Settings
import com.example.cpudefense.Stage
import com.example.cpudefense.gameElements.SevenSegmentDisplay


class WelcomeActivity : AppCompatActivity() {
    private var info: PackageInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)  // method of AppCompatActivity
        setContentView(R.layout.activity_welcome)
        info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)

        // migrate preferences files, if needed
        val prefsLegacy = getSharedPreferences(Persistency.filename_legacy, MODE_PRIVATE)
        val prefsSettings = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        Settings().migrateSettings(prefsLegacy, prefsSettings)
    }

    private var gameState: String? = null
    private var nextLevelToPlay = Stage.Identifier()
    private var maxLevel = Stage.Identifier()
    private var turboSeriesAvailable = false
    private var endlessSeriesAvailable = false

    private fun determineLevels(prefs: SharedPreferences) {
        maxLevel.series = prefs.getInt("MAXSERIES", 1)
        maxLevel.number = prefs.getInt("MAXSTAGE", 0)
        nextLevelToPlay.series = prefs.getInt("LASTSERIES", 1)
        nextLevelToPlay.number = prefs.getInt("LASTSTAGE", 0)
        turboSeriesAvailable = prefs.getBoolean("TURBO_AVAILABLE", false)
        endlessSeriesAvailable = prefs.getBoolean("ENDLESS_AVAILABLE", false)
    }

    private fun showLevelReached()
            /** displays the max level reached so far as graphical display */
    {
        // display as graphics:
        var displayLit = true
        val display =
            SevenSegmentDisplay(4, (80 * resources.displayMetrics.scaledDensity).toInt(), this)
        val imageView = findViewById<ImageView>(R.id.sevenSegmentDisplay)
        if (maxLevel.number == 0)
            displayLit = false
        when (maxLevel.series) {
            GameMechanics.SERIES_NORMAL -> imageView.setImageBitmap(
                display.getDisplayBitmap(
                    maxLevel.number,
                    SevenSegmentDisplay.LedColors.GREEN,
                    displayLit
                )
            )
            GameMechanics.SERIES_TURBO -> imageView.setImageBitmap(
                display.getDisplayBitmap(
                    maxLevel.number,
                    SevenSegmentDisplay.LedColors.YELLOW,
                    displayLit
                )
            )
            else -> imageView.setImageBitmap(
                display.getDisplayBitmap(
                    maxLevel.number,
                    SevenSegmentDisplay.LedColors.RED,
                    displayLit
                )
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun showMaxLevelInfo(v: View) {
        /** displays the max level reached so far as graphical display */
        val seriesName = when (maxLevel.series)
        {
            GameMechanics.SERIES_NORMAL -> getString(R.string.name_series_1)
            GameMechanics.SERIES_TURBO -> getString(R.string.name_series_2)
            GameMechanics.SERIES_ENDLESS -> getString(R.string.name_series_3)
            else -> "???"  // shouldn't happen
        }
        val textToDisplay = getString(R.string.stage_reached).format(seriesName, maxLevel.number)
        Toast.makeText(this, textToDisplay, Toast.LENGTH_LONG).show()

    }

    private fun migrateLevelInfo(oldPrefs: SharedPreferences, newPrefs: SharedPreferences)
            /** gets the level info out of the "old" prefs file and puts it into the "new" one.
             * The keys are deleted from oldPrefs.
             * This function is used for upgrade to version 1.44.
             */
    {
        determineLevels(newPrefs)
        if (maxLevel.series==1 && maxLevel.number==0)
        {
            // no level info, try to use old values
            determineLevels(oldPrefs)
            newPrefs.edit().apply {
                putInt("MAXSERIES", maxLevel.series)
                putInt("MAXSTAGE", maxLevel.number)
                putInt("LASTSERIES", nextLevelToPlay.series)
                putInt("LASTSTAGE", nextLevelToPlay.number)
                putBoolean("TURBO_AVAILABLE", turboSeriesAvailable)
                putBoolean("ENDLESS_AVAILABLE", turboSeriesAvailable)
                apply()
            }
            oldPrefs.edit().apply {
                remove("MAXSERIES")
                remove("MAXSTAGE")
                remove("LASTSERIES")
                remove("LASTSTAGE")
                remove("TURBO_AVAILABLE")
                remove("ENDLESS_AVAILABLE")
                remove("STATUS")  // has also been migrated
                apply()
            }
        }
    }

    private fun setupButtons() {
        val prefsState = getSharedPreferences(Persistency.filename_state, MODE_PRIVATE)
        val prefsLegacy = getSharedPreferences(Persistency.filename_legacy, MODE_PRIVATE)
        gameState = prefsState.getString("STATUS", "")
        determineLevels(prefsState)
        if (maxLevel.series == 1 && maxLevel.number == 0)  // no level info, try other file
            migrateLevelInfo(prefsLegacy, prefsState)
        showLevelReached()
        val buttonResume = findViewById<Button>(R.id.continueGameButton)
        when {
            maxLevel.number == 0 -> buttonResume.text = getString(R.string.button_start_game)
            gameState == "running" -> buttonResume.text = getString(R.string.button_resume)
            gameState == "complete" -> {
                buttonResume.text = getString(R.string.play_level_x).format(nextLevelToPlay.number)
            }
            else -> buttonResume.isEnabled = false
        }
        // uncomment if there is a message to display
        // showVersionMessage()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        setupButtons()
    }

    fun resumeGame(@Suppress("UNUSED_PARAMETER") v: View) {
        val intent = Intent(this, GameActivity::class.java)
        when {
            maxLevel.number == 0 -> {
                // start new game
                intent.putExtra("RESET_PROGRESS", true)
                intent.putExtra("START_ON_STAGE", 1)
                intent.putExtra("START_ON_SERIES", 0)
                intent.putExtra("CONTINUE_GAME", false)
                startActivity(intent)
            }
            gameState == "running" -> {
                intent.putExtra("RESUME_GAME", true)
                startActivity(intent)
            }
            else -> {
                intent.putExtra("START_ON_STAGE", nextLevelToPlay.number)
                intent.putExtra("START_ON_SERIES", nextLevelToPlay.series)
                intent.putExtra("CONTINUE_GAME", false)
                startActivity(intent)
            }
        }
    }

    fun startLevelSelection(@Suppress("UNUSED_PARAMETER") v: View) {
        val intent = Intent(this, LevelSelectActivity::class.java)
        intent.putExtra("TURBO_AVAILABLE", turboSeriesAvailable)
        intent.putExtra("ENDLESS_AVAILABLE", endlessSeriesAvailable)
        intent.putExtra("NEXT_SERIES", nextLevelToPlay.series)
        startActivity(intent)
    }

    fun displaySettingsDialog(@Suppress("UNUSED_PARAMETER") v: View) {
        val intent = Intent(this, SettingsActivity::class.java)
        intent.putExtra("MAXSERIES", maxLevel.series)
        startActivity(intent)
        setupButtons()
    }

    fun displayAboutDialog(@Suppress("UNUSED_PARAMETER") v: View) {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

    @Suppress("unused")
    fun showVersionMessage()
    /** display version message, if not already displayed earlier */
    {
        val prefs = getSharedPreferences(Persistency.filename_state, MODE_PRIVATE)
        info?.let {
            val messageDisplayed = prefs.getString("VERSIONMESSAGE_SEEN", "")
            if (messageDisplayed != it.versionName) {
                showMessageOfTheDay()
                with(prefs.edit()) {
                    putString("VERSIONMESSAGE_SEEN", it.versionName)
                    commit()
                }
            }
        }
    }

    private fun showMessageOfTheDay()
    {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_dialog_message_of_the_day)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)
        dialog.findViewById<TextView>(R.id.question).text = resources.getText(R.string.ZZ_message_of_the_day)
        val button1 = dialog.findViewById<Button>(R.id.button1)
        button1?.text = resources.getText(R.string.close)
        button1?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun exitActivity(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }
}