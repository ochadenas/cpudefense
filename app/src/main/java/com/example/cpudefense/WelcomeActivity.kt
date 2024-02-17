package com.example.cpudefense

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cpudefense.gameElements.SevenSegmentDisplay


class WelcomeActivity : AppCompatActivity()
{
    // TODO: do not use standard buttons, but define styles for active and inactive versions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
    }

    private var gameState: String? = null
    private var nextLevelToPlay = Stage.Identifier()
    private var maxLevel = Stage.Identifier()
    private var turboSeriesAvailable = false
    private var endlessSeriesAvailable = false

    private fun determineLevels(prefs: SharedPreferences)
    {
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
        val display = SevenSegmentDisplay(2, (80 * resources.displayMetrics.scaledDensity).toInt(), this)
        val imageView = findViewById<ImageView>(R.id.sevenSegmentDisplay)
        if (maxLevel.number == 0)
            displayLit = false
        when (maxLevel.series)
        {
            1 -> imageView.setImageBitmap(display.getDisplayBitmap(maxLevel.number, SevenSegmentDisplay.LedColors.GREEN, displayLit))
            2 -> imageView.setImageBitmap(display.getDisplayBitmap(maxLevel.number, SevenSegmentDisplay.LedColors.YELLOW, displayLit))
            else -> imageView.setImageBitmap(display.getDisplayBitmap(maxLevel.number, SevenSegmentDisplay.LedColors.RED, displayLit))
        }
    }

    fun showMaxLevelInfo(v: View)
    {
        /** displays the max level reached so far as graphical display */
        val seriesName = if (maxLevel.series == 2) getString(R.string.name_series_2) else getString(R.string.name_series_1)
        val textToDisplay = getString(R.string.stage_reached).format(seriesName, maxLevel.number)
        Toast.makeText(this, textToDisplay, Toast.LENGTH_LONG).show()

    }

    private fun setupButtons()
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        gameState = prefs.getString("STATUS", "")
        determineLevels(prefs)
        showLevelReached()
        val buttonResume = findViewById<Button>(R.id.continueGameButton)
        when (gameState)
        {
            "running" -> buttonResume.text = getString(R.string.button_resume)
            "complete" -> {
                buttonResume.text = getString(R.string.play_level_x).format(nextLevelToPlay.number)
            }
            else -> buttonResume.isEnabled = false
        }
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        setupButtons()
    }

    fun resumeGame(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, MainGameActivity::class.java)
        if (gameState == "running") {
            intent.putExtra("RESUME_GAME", true)
            startActivity(intent)
        }
        else
        {
            intent.putExtra("START_ON_STAGE", nextLevelToPlay.number)
            intent.putExtra("START_ON_SERIES", nextLevelToPlay.series)
            intent.putExtra("CONTINUE_GAME", false)  // TODO: wie heißt es nun wirklich?
            startActivity(intent)
        }
    }

    fun startLevelSelection(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, LevelSelectActivity::class.java)
        intent.putExtra("TURBO_AVAILABLE", turboSeriesAvailable)
        intent.putExtra("ENDLESS_AVAILABLE", endlessSeriesAvailable)
        intent.putExtra("NEXT_SERIES", nextLevelToPlay.series)
        startActivity(intent)
    }

    fun displaySettingsDialog(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        setupButtons()
    }

    fun displayAboutDialog(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }
}