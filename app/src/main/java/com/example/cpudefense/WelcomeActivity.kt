package com.example.cpudefense

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


class WelcomeActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        val versionName = "Version %s ".format(info.versionName)
        val view = findViewById<TextView>(R.id.versionText)
        view?.setText(versionName)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }

    fun resumeGame(v: View)
    {
        val intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("RESUME_GAME", true)
        startActivity(intent)
    }

    fun startLevelSelection(v: View)
    {
        val intent = Intent(this, LevelSelectActivity::class.java)
        startActivity(intent)
    }

    fun displaySettingsDialog(v: View)
    {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    fun startNewGame(v: View)
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
                }
                .setNegativeButton("No") { dialog, id -> dialog.dismiss() }
        val alert = builder.create()
        alert.show()
    }


}