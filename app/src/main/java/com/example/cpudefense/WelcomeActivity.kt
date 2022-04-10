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
        prepareStageSelector()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        prepareStageSelector()
    }

    fun startGame(v: View)
    {
        var intent = Intent(this, MainGameActivity::class.java)
        val startLevel = findViewById<NumberPicker>(R.id.stageSelect)?.value ?: 1
        intent.putExtra("START_ON_STAGE", startLevel)
        intent.putExtra("CONTINUE_GAME", false)
        startActivity(intent)
    }

    fun continueGame(v: View)
    {
        var intent = Intent(this, MainGameActivity::class.java)
        val startLevel = findViewById<NumberPicker>(R.id.stageSelect)?.value ?: 1
        intent.putExtra("CONTINUE_GAME", true)
        startActivity(intent)
    }

    fun prepareStageSelector()
    {
        var prefs = getSharedPreferences(getString(R.string.pref_filename), Context.MODE_PRIVATE)
        val maxStage = prefs.getInt("MAXSTAGE", 1)
        val currentStage = prefs.getInt("LASTSTAGE", 1)
        val stageSelector = findViewById(R.id.stageSelect) as NumberPicker
        stageSelector.maxValue = maxStage+1
        stageSelector.minValue = 1
        stageSelector.value = currentStage+1
        stageSelector.wrapSelectorWheel = false
    }

}