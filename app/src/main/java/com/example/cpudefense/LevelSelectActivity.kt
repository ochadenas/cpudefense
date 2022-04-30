package com.example.cpudefense

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap

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
        var thumbnails = Persistency(null).loadLevelThumbnails(prefs)

        if (levels == null)
            levels = hashMapOf(0 to Stage.Summary())  // create empty first level

        for ((level, summary) in levels?.entries!!)
        {
            val levelEntryView = Button(this)
            var textString = getString(R.string.level_entry).format(level)
            if (summary.coinsMaxAvailable > 0)
                textString = textString.plus("\n%d of %d coins got.".format(summary.coinsGot, summary.coinsMaxAvailable))
            levelEntryView.text = textString
            levelEntryView.textSize = 32f
            levelEntryView.isAllCaps = false
            levelEntryView.setPadding(20, 0, 0, 0)
            levelEntryView.setBackgroundColor(Color.BLACK)
            levelEntryView.setGravity(Gravity.START)

            addLevelIcon(levelEntryView, thumbnails?.get(level))

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

    fun addLevelIcon(view: TextView, encodedImage: String?)
            /** add an icon representing the network of the level
             * @param view: the level entry
             */
    {
        // reconstruct bitmap from string saved in preferences
        var snapshot: Bitmap? = null
        try {
            val decodedBytes: ByteArray = Base64.decode(encodedImage, Base64.DEFAULT)
            snapshot = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
        catch(e: Exception)
        {
            // unable to get a level snapshot, for whatever reason
        }

        // add bitmap to text view
        val iconPadding = 10
        val iconSize = Game.levelSnapshotIconSize + iconPadding
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        snapshot?.let {
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
        var intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("START_ON_STAGE", selectedLevel)
        intent.putExtra("CONTINUE_GAME", false)
        startActivity(intent)
        finish()
    }
}