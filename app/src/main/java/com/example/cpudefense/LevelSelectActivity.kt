package com.example.cpudefense

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class LevelSelectActivity : AppCompatActivity() {
    var levels: HashMap<Int, Stage.Summary>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level)
        prepareStageSelector()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        prepareStageSelector()
    }

    fun prepareStageSelector()
    {
        val listView = findViewById<LinearLayout>(R.id.levelList)
        val prefs = getSharedPreferences(getString(R.string.pref_filename), MODE_PRIVATE)
        levels = Persistency(null).loadLevels(prefs)

        for ((level, summary) in levels?.entries!!)
        {
            val levelEntryView = TextView(this)
            val hasWon: String = if (summary.won) "*" else " "
            levelEntryView.text = "Level %d %s\n%d of %d coins got.".format(level, hasWon, summary.coinsGot, summary.coinsMaxAvailable)
            levelEntryView.textSize = 24f
            levelEntryView.setTextAppearance(this, R.style.TextAppearance_AppCompat_Medium)
            levelEntryView.setTextColor(resources.getColor(R.color.text_green))
            listView.addView(levelEntryView)
        }
    }

}