package com.example.cpudefense

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.cpudefense.MainGameActivity.GameActivityStatus.UNDETERMINED


class WelcomeActivity : AppCompatActivity()
{
    // TODO: do not use standard buttons, but define styles for active and inactive versions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        val versionName = "Version %s ".format(info.versionName)
        val gameVersion = findViewById<TextView>(R.id.versionText)
        gameVersion?.text = versionName
    }

    var gameState: String? = null
    var nextLevelToPlay = 1

    private fun setupButtons()
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        gameState = prefs.getString("STATUS", "")
        val buttonResume = findViewById<Button>(R.id.continueGameButton)
        when (gameState)
        {
            "running" -> buttonResume.text = getString(R.string.button_resume)
            "complete" -> {
                nextLevelToPlay = prefs.getInt("LASTSTAGE", 0)
                if (nextLevelToPlay < prefs.getInt("MAXSTAGE", 1))
                    nextLevelToPlay += 1
                buttonResume.text = getString(R.string.play_level_x).format(nextLevelToPlay)
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
            intent.putExtra("START_ON_STAGE", nextLevelToPlay)
            intent.putExtra("CONTINUE_GAME", false)  // TODO: wie heißt es nun wirklich?
            startActivity(intent)
        }
    }

    fun startLevelSelection(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, LevelSelectActivity::class.java)
        startActivity(intent)
    }

    fun displaySettingsDialog(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, SettingsActivity::class.java)
    }

    fun startNewGame(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Really start new game? This will reset all your progress.")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    val intent = Intent(this, MainGameActivity::class.java)
                    intent.putExtra("RESET_PROGRESS", true)
                    intent.putExtra("START_ON_STAGE", 1)
                    intent.putExtra("CONTINUE_GAME", false)
                    startActivity(intent)
                    setupButtons()
                }
                .setNegativeButton("No") { dialog, id -> dialog.dismiss() }
        val alert = builder.create()
        alert.show()
    }
}