package com.example.cpudefense

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LevelSelectActivity : AppCompatActivity() {
    var levels: HashMap<Int, Stage.Summary>? = null
    var selectedLevelView: Button? = null
    var selectedLevel: Int = 0

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
        levels = Persistency(null).loadLevelSummaries(prefs)

        if (levels == null)
            levels = hashMapOf(0 to Stage.Summary())  // create empty first level
        levels?.let {
            val maxLevelCompleted = prefs?.getInt("MAXSTAGE", 0)  ?: 0
            if (maxLevelCompleted < Game.maxLevelAvailable)
                it[maxLevelCompleted + 1] = Stage.Summary()  // create an empty level if necessary

            for ((level, summary) in it.entries)
            {
                val levelEntryView = Button(this)
                var textString = getString(R.string.level_entry).format(level)
                val coinsMaxAvailable = summary.coinsAvailable + summary.coinsGot
                if (coinsMaxAvailable > 0)
                    textString = textString.plus("\n%d of %d coins got.".format(summary.coinsGot, coinsMaxAvailable))
                levelEntryView.text = textString
                levelEntryView.textSize = 32f
                levelEntryView.isAllCaps = false
                levelEntryView.setPadding(20, 0, 0, 0)
                levelEntryView.setBackgroundColor(Color.BLACK)
                levelEntryView.gravity = Gravity.START

                val thumbnail = Persistency(null).loadThumbnailOfLevel(prefs, level)
                addLevelIcon(levelEntryView, thumbnail)

                levelEntryView.setTextAppearance(this, R.style.TextAppearance_AppCompat_Medium)
                if (summary.won)
                    levelEntryView.setTextColor(resources.getColor(R.color.text_green))
                else
                    levelEntryView.setTextColor(resources.getColor(R.color.text_amber))
                levelEntryView.isClickable = true
                levelEntryView.setOnClickListener { onLevelSelect(levelEntryView, level) }
                listView.addView(levelEntryView)
            }
        }

    }

    fun addLevelIcon(view: TextView, icon: Bitmap?)
            /** add an icon representing the network of the level
             * @param view: the level entry
             */
    {
        // add bitmap to text view
        val iconPadding = 10
        val iconSize = Game.levelSnapshotIconSize + iconPadding
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        icon?.let {
            val canvas = Canvas(bitmap)
            val paint = Paint()
            canvas.drawBitmap(it, null, Rect(iconPadding,iconPadding, iconSize-iconPadding, iconSize-iconPadding), paint)
        }
        view.setCompoundDrawablesWithIntrinsicBounds(BitmapDrawable(resources, bitmap), null, null, null)
    }

    fun onLevelSelect(v: View, level: Int)
    {
        selectedLevel = level

        // clear outline
        selectedLevelView?.setBackgroundColor(Color.BLACK)

        // draw outline around selected button
        selectedLevelView = v as Button
        selectedLevelView?.setBackgroundResource(R.drawable.button_border)
    }

    fun startGame(v: View)
    {
        if (selectedLevel == 0)
            return
        val intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("START_ON_STAGE", selectedLevel)
        intent.putExtra("CONTINUE_GAME", false)
        startActivity(intent)
        finish()
    }
}