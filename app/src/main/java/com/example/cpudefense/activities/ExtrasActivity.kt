package com.example.cpudefense.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.extras.AchievementType
import com.example.cpudefense.extras.ActivityWithAchievements
import com.example.cpudefense.extras.MemoryMapView
import com.example.cpudefense.extras.PlayerAchievements
import com.example.cpudefense.extras.SevenSegmentClock
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.utils.TimeStamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExtrasActivity : ActivityWithAchievements, AppCompatActivity()
{
    override var achievements = PlayerAchievements()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        supportActionBar?.hide()
        setContentView(R.layout.activity_extras)
        findViewById<View>(R.id.extras_layout)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView, ::handleInsets)
        }
        findViewById<ViewPager2>(R.id.viewPager)?.let{
            it.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount() = 4
                override fun createFragment(p: Int): Fragment {
                    return when (p) {
                        0 -> LevelStatisticsFragment()
                        3 -> AboutFragment()
                        1 -> AchievementsFragment()
                        else -> ExtrasBasicFragment()
                    }
                }
            }
            it.setCurrentItem(2, false)
        }
        val dots = listOf(findViewById<View>(R.id.led1), findViewById(R.id.led2), findViewById(R.id.led3), findViewById(R.id.led4))
        findViewById<ViewPager2>(R.id.viewPager).registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, view ->
                    if (i == position) {
                        view.background = ContextCompat.getDrawable(this@ExtrasActivity, R.drawable.shape_led_red)
                    } else {
                        view.background = ContextCompat.getDrawable(this@ExtrasActivity, R.drawable.shape_led_off)
                    }
                }
            }
        })
    }

    fun handleInsets(@Suppress("UNUSED_PARAMETER")view: View, windowInsets: WindowInsetsCompat): WindowInsetsCompat
    {
        windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        return windowInsets
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }

    fun wiki(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://github.com/ochadenas/cpudefense/wiki/Chip-Defense".toUri())
        try {
            grantAchievement(AchievementType.WIKI)
            Toast.makeText(this, resources.getString(R.string.message_open_in_browser), Toast.LENGTH_SHORT).show()
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }

    fun releaseNotes(@Suppress("UNUSED_PARAMETER") v: View)
    {
        grantAchievement(AchievementType.VERSIONINFO)
        val url = resources.getString(R.string.url_releasenotes_base).format(resources.getString(R.string.url_releasenotes_localized))
        Toast.makeText(this, resources.getString(R.string.message_open_in_browser), Toast.LENGTH_SHORT).show()
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }

    fun startEditor(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val intent = Intent(this, EditorActivity::class.java)
        try {
            startActivity(intent)
        }
        catch (_: Exception) {}
    }

    fun grantAchievement(type: AchievementType)
    {
        achievements.let {
            it.grant(type)
            val newAchievements = it.processPending(this)
            when (newAchievements.size)
            {
                1 -> { Toast.makeText(this, R.string.achievement_gained_toast, Toast.LENGTH_SHORT). show() }
                2 -> { Toast.makeText(this, R.string.achievements_gained_toast, Toast.LENGTH_SHORT). show() }
                else -> {}
            }
        }
    }

}

class LevelStatisticsFragment : Fragment() {
    var memoryMapView: MemoryMapView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_memorybank, container, false)
        memoryMapView = view.findViewById(R.id.map_view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        memoryMapView?.let { setCaption(it.totalCashSum.toInt()) }
    }

    fun setCaption(cash: Int)
    /** updates the information in the caption below the map */
    {
        val cashString = ScoreBoard.informationToString(cash)
        view?.findViewById<TextView>(R.id.caption_view)?.text =
            getString(R.string.total_information_gained, cashString)
    }
}

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        view?.let{ rootView ->
            val activity = requireActivity()
            val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES)
            val versionView: TextView = rootView.findViewById(R.id.fragment_about_version)
            versionView.text = getString(R.string.about_version).format(info.versionName)
        }
        return view
    }
}

/** builds the page that lists all the achievements gained so far.
 * Sets the summary information and fills a list with the achievements read from file.
 */
class AchievementsFragment : Fragment() {
    var rootView: View? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        rootView = inflater.inflate(R.layout.fragment_achievements, container, false)
        rootView?.let{ rootView ->
            val activity = requireActivity()
            // get the achievements data
            Persistency(activity).loadAchievements()
        }
        return rootView
    }

    override fun onResume() {
        super.onResume()
        fillAchievementList()
    }

    fun fillAchievementList() {
        val achievements = (activity as ActivityWithAchievements).achievements
        rootView?.findViewById<TextView>(R.id.achievements_summary)?.apply {
            when (achievements.obtainedCount)
            {
                0 -> text = context.getString(R.string.achievements_summary_0, achievements.totalCount)
                1 -> text = context.getString(R.string.achievements_summary_1, achievements.totalCount)
                else ->  text = requireActivity().resources.getString(R.string.achievements_summary).format(
                        achievements.obtainedCount, achievements.totalCount, achievements.obtainedPercentage)
            }
        }
        rootView?.findViewById<LinearLayout>(R.id.achievements_list)?.apply {
            removeAllViews()
            achievements.confirmed.values.sortedByDescending { it.granted }.forEach { entry ->
                TextView(context).apply {
                    text = PlayerAchievements.getText(this.resources, entry.type)
                    setTextColor(resources.getColor(R.color.text_blue, null))
                    textSize = (resources.getDimension(R.dimen.extras_text_size))
                }.also { addView(it) }
                TextView(context).apply {
                    entry.granted?.let {
                        val t = TimeStamp(resources, it).apply { convertToReadable() }
                        text = context.getString(R.string.achievement_obtained_on, t.readableDate, t.readableTime)
                    }
                    setTextColor(resources.getColor(R.color.text_blue, null))
                    textSize = (resources.getDimension(R.dimen.extras_details_size))
                }.also { addView(it) }
            }
        }
    }

}

class ExtrasBasicFragment : Fragment() {
    lateinit var contentsView: View
    lateinit var clock: SevenSegmentClock
    val isActive: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        contentsView = inflater.inflate(R.layout.extras_basic, container, false)
        if (!GameMechanics.enableEditor)
            contentsView.findViewById<ConstraintLayout>(R.id.button_container)?.let {
                it.removeView(it.findViewById(R.id.button_editor))
            }
        clock = SevenSegmentClock(resources.getDimension(R.dimen.sevensegment_display_height).toInt(),
                            requireActivity() as ExtrasActivity)
        updateClock()
        return contentsView
    }

    fun updateClock()
    {
        val clockView = contentsView.findViewById<ImageView>(R.id.seven_segment_clock)
        clockView?.setImageBitmap(clock.getDisplayBitmap())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val job = lifecycleScope.launch {
            while (isActive) {
                updateClock()
                delay(450)
            }
        }

        // stop clock when fragment gets destroyed */
        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                job.cancel()
            }
        })
    }
}
