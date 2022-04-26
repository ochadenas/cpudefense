package com.example.cpudefense

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.NumberPicker

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }

    fun startGame(v: View)
    {
        val prefs = getSharedPreferences(getString(R.string.pref_filename), Context.MODE_PRIVATE)
        val maxStage = prefs.getInt("MAXSTAGE", 1)
        val currentStage = prefs.getInt("LASTSTAGE", 1)
        val startLevel = currentStage

        val intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("START_ON_STAGE", startLevel)
        intent.putExtra("CONTINUE_GAME", false)
        startActivity(intent)
    }

    fun continueGame(v: View)
    {
        val intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("CONTINUE_GAME", true)
        startActivity(intent)
    }

    fun startLevelSelection(v: View)
    {
        val intent = Intent(this, LevelSelectActivity::class.java)
        startActivity(intent)
    }


}