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
import com.google.android.material.tabs.TabLayout

@Suppress("DEPRECATION")
class LevelSelectActivity : AppCompatActivity() {
    private var levels: HashMap<Int, Stage.Summary> = HashMap()
    private var selectedLevelView: Button? = null
    private var selectedLevel: Int = 0
    private var selectedSeries: Int = 0
    private var isTurboAvailable = false
    private var isEndlessAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isTurboAvailable = intent.getBooleanExtra("TURBO_AVAILABLE", false)
        isEndlessAvailable = intent.getBooleanExtra("ENDLESS_AVAILABLE", false)
        setContentView(R.layout.activity_level)
        setupSelector()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?)
    {
        super.onActivityReenter(resultCode, data)
        isTurboAvailable = intent.getBooleanExtra("TURBO_AVAILABLE", false)
        isEndlessAvailable = intent.getBooleanExtra("ENDLESS_AVAILABLE", false)
        setupSelector()
    }

    private fun setupSelector()
    {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        if (GameMechanics.makeAllLevelsAvailable)
        {
            isTurboAvailable = true
            isEndlessAvailable = true
        }
        tabLayout.setOnTabSelectedListener(
            (object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab) {
                    prepareStageSelector(tab.position+1)
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    // Write code to handle tab reselect
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    // Write code to handle tab reselect
                }
            })
        )
        val currentSeries = intent.getIntExtra("NEXT_SERIES", GameMechanics.SERIES_NORMAL)
        prepareStageSelector(currentSeries)
        // set the active tab depending on the current series
        val tab = tabLayout.getTabAt(currentSeries-1)
        tab?.select()
    }

    private fun nextLevelPossible(level: Int, series: Int): Boolean
            /** returns whether this level can shown in the list.
             * For the first two series, they are limited to maxLevelAvailable.
             * In "endless" there is no limit.
             */
    {
        return ((series == GameMechanics.SERIES_ENDLESS) || (level < GameMechanics.maxLevelAvailable))
    }

    private fun prepareStageSelector(series: Int)
            /**
             * Prepare stage selector for the given series. Either populate the level list
             * with the entries for each stage, or display a message.
             *
             * @param series The level series (1, 2, ...)
             */
    {
        val listView = findViewById<LinearLayout>(R.id.levelList)
        listView.removeAllViews()
        selectedSeries = series
        when (series)
        {
            GameMechanics.SERIES_NORMAL -> {
                levels = Persistency(this).loadStageSummaries(GameMechanics.SERIES_NORMAL)
                populateStageList(
                        listView, levels, GameMechanics.SERIES_NORMAL, resources.getColor(R.color.text_green),
                        resources.getColor(R.color.text_lightgreen)
                )
            }
            GameMechanics.SERIES_TURBO-> {
                if (isTurboAvailable) {
                    levels = Persistency(this).loadStageSummaries(GameMechanics.SERIES_TURBO)
                    populateStageList(
                            listView, levels, GameMechanics.SERIES_NORMAL, resources.getColor(R.color.text_amber),
                            resources.getColor(R.color.text_lightamber)
                    )
                }
                else
                {
                    val textView = TextView(this)
                    textView.text = getString(R.string.message_series_unavailable)
                    textView.textSize = 8f * resources.displayMetrics.scaledDensity
                    textView.isAllCaps = false
                    textView.setPadding(20, 0, 0, 0)
                    textView.setBackgroundColor(Color.BLACK)
                    textView.setTextColor(resources.getColor(R.color.text_white))
                    textView.gravity = Gravity.START
                    listView.addView(textView)
                }
            }
            GameMechanics.SERIES_ENDLESS -> {
                if (isEndlessAvailable) {
                    levels = Persistency(this).loadStageSummaries(GameMechanics.SERIES_ENDLESS)
                    populateStageList(
                            listView, levels, GameMechanics.SERIES_ENDLESS, resources.getColor(R.color.text_red),
                            resources.getColor(R.color.text_lightred)
                    )
                }
                else
                {
                    val textView = TextView(this)
                    textView.text = getString(R.string.message_endless_unavailable)
                    textView.textSize = 8f * resources.displayMetrics.scaledDensity
                    textView.isAllCaps = false
                    textView.setPadding(20, 0, 0, 0)
                    textView.setBackgroundColor(Color.BLACK)
                    textView.setTextColor(resources.getColor(R.color.text_white))
                    textView.gravity = Gravity.START
                    listView.addView(textView)
                }
            }
        }
    }

    private fun populateStageList(
        listView: LinearLayout, stageSummary: HashMap<Int, Stage.Summary>, series: Int,
        colorFinished: Int, colorUnfinished: Int
    )
            /**
             * Populate the scrollable list for a given set of levels, depending on the series.
             *
             * @param listView The list where the entries should be added
             * @param stageSummary The dictionary with the level data. May be an empty set.
             */
    {
        /* create empty first and last stage, if necessary */
        if (stageSummary.isEmpty())
            stageSummary[1] = Stage.Summary()  // create empty first level
        // try to determine whether a new level has been added,
        // and it becomes available directly
        val highestLevelInList = ArrayList(stageSummary.keys).last()
        if (nextLevelPossible(highestLevelInList, series) && (stageSummary[highestLevelInList]?.won == true))
            stageSummary[highestLevelInList+1] = Stage.Summary()

        for ((level, summary) in stageSummary.entries)
        {
            val levelEntryView = Button(this)
            var textString = getString(R.string.level_entry).format(level)
            val coinsMaxAvailable = when {
                // this is a hack to handle levels where coinsMaxAvailable is not set correctly
                summary.coinsMaxAvailable>0 -> summary.coinsMaxAvailable
                else -> summary.coinsAvailable + summary.coinsGot
            }
            if (coinsMaxAvailable > 0)
                textString = textString.plus("\n%d of %d coins got.".format(summary.coinsGot, coinsMaxAvailable))
            levelEntryView.text = textString
            levelEntryView.textSize = 32f
            levelEntryView.isAllCaps = false
            levelEntryView.setPadding(20, 0, 0, 0)
            levelEntryView.setBackgroundColor(Color.BLACK)
            levelEntryView.gravity = Gravity.START

            val thumbnail = Persistency(this).loadThumbnailOfLevel(level, series)
            addLevelIcon(levelEntryView, thumbnail)

            levelEntryView.setTextAppearance(this, R.style.TextAppearance_AppCompat_Medium)
            // choose color of text:
            @Suppress("SimplifyBooleanWithConstants") // no linter warning, please
            when
            {
                (summary.won == true && (summary.coinsGot < summary.coinsMaxAvailable))
                -> levelEntryView.setTextColor(colorUnfinished)
                summary.won == true -> levelEntryView.setTextColor(colorFinished)
                else -> levelEntryView.setTextColor(resources.getColor(R.color.text_white))
            }
            levelEntryView.isClickable = true
            levelEntryView.setOnClickListener { onLevelSelect(levelEntryView, level) }
            listView.addView(levelEntryView)
            if (!nextLevelPossible(level, series))
                break
        }
    }

    private fun addLevelIcon(view: TextView, icon: Bitmap?)
            /** add an icon representing the network of the level
             * @param view: the level entry
             */
    {
        // add bitmap to text view
        val iconPadding = 10
        val iconSize = GameMechanics.levelSnapshotIconSize + iconPadding
        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        icon?.let {
            val canvas = Canvas(bitmap)
            val paint = Paint()
            canvas.drawBitmap(it, null, Rect(iconPadding,iconPadding, iconSize-iconPadding, iconSize-iconPadding), paint)
        }
        view.setCompoundDrawablesWithIntrinsicBounds(BitmapDrawable(resources, bitmap), null, null, null)
    }

    private fun onLevelSelect(v: View, level: Int)
    /** make the list entry visibly active when the user touches it */
    {
        selectedLevel = level

        // clear outline
        selectedLevelView?.setBackgroundColor(Color.BLACK)

        // draw outline around selected button
        selectedLevelView = v as Button
        selectedLevelView?.setBackgroundResource(R.drawable.button_border_white)
    }

    fun startGame(@Suppress("UNUSED_PARAMETER") v: View)
    /** called when the Start Game button is pushed */
    {
        if (selectedLevel == 0)
            return
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("START_ON_STAGE", selectedLevel)
        intent.putExtra("START_ON_SERIES", selectedSeries)
        intent.putExtra("CONTINUE_GAME", false)
        startActivity(intent)
        finish()
    }
}