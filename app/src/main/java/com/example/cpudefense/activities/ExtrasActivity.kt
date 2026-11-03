package com.example.cpudefense.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.cpudefense.databinding.ActivityExtrasBinding
import com.example.cpudefense.databinding.ExtrasBasicBinding
import com.example.cpudefense.databinding.FragmentAboutBinding
import com.example.cpudefense.databinding.FragmentAchievementsBinding
import com.example.cpudefense.databinding.FragmentMemorybankBinding
import com.example.cpudefense.extras.AchievementType
import com.example.cpudefense.extras.ActivityWithAchievements
import com.example.cpudefense.extras.MemoryMapView
import com.example.cpudefense.extras.PlayerAchievements
import com.example.cpudefense.extras.SevenSegmentClock
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.utils.TimeStamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ExtrasActivity : ActivityWithAchievements, AppCompatActivity()
{
    override var achievements = PlayerAchievements()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        supportActionBar?.hide()
        val binding = ActivityExtrasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.extrasLayout.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView, ::handleInsets)
        }
        binding.viewPager.let{
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
        val dots = listOf(binding.led1, binding.led2, binding.led3, binding.led4)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
                1 -> { Toast.makeText(this, R.string.achievement_gained_toast, Toast.LENGTH_SHORT).show() }
                2 -> { Toast.makeText(this, R.string.achievements_gained_toast, Toast.LENGTH_SHORT).show() }
                else -> {}
            }
        }
    }

}

class LevelStatisticsFragment : Fragment() {
    private lateinit var memoryMapView: MemoryMapView
    private lateinit var fragmentMemorybankBinding: FragmentMemorybankBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        fragmentMemorybankBinding = FragmentMemorybankBinding.inflate(layoutInflater, container, false)
        memoryMapView = fragmentMemorybankBinding.mapView
        return fragmentMemorybankBinding.root
    }

    override fun onResume() {
        super.onResume()
        memoryMapView.let { setCaption(it.totalCashSum.toInt()) }
    }

    /** updates the information in the caption below the map */
    private fun setCaption(cash: Int)
    {
        val cashString = ScoreBoard.informationToString(cash)
        fragmentMemorybankBinding.captionView.text =
            getString(R.string.total_information_gained, cashString)
    }
}

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding: FragmentAboutBinding = FragmentAboutBinding.inflate(layoutInflater, container, false)
        val activity = requireActivity()
        val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES)
        val versionView: TextView = binding.fragmentAboutVersion
        versionView.text = getString(R.string.about_version).format(info.versionName)
        return view
    }
}

/** builds the page that lists all the achievements gained so far.
 * Sets the summary information and fills a list with the achievements read from file.
 */
class AchievementsFragment : Fragment() {
    private lateinit var binding: FragmentAchievementsBinding

    /** prepares the view. The actual achievement list is done in onResume(),
     * because achievements may change during the activity also.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = FragmentAchievementsBinding.inflate(layoutInflater, container, false)
        Persistency(requireActivity()).loadAchievements()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        fillAchievementList()
    }

    fun fillAchievementList() {
        val achievements = (activity as ActivityWithAchievements).achievements
        binding.achievementsSummary.apply {
            when (achievements.obtainedCount)
            {
                0 -> text = context.getString(R.string.achievements_summary_0, achievements.totalCount)
                1 -> text = context.getString(R.string.achievements_summary_1, achievements.totalCount)
                else ->  text = requireActivity().resources.getString(R.string.achievements_summary).format(
                        achievements.obtainedCount, achievements.totalCount, achievements.obtainedPercentage)
            }
        }
        binding.achievementsList.apply {
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
    private lateinit var binding: ExtrasBasicBinding
    private lateinit var clock: SevenSegmentClock
    private val isActive: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        binding = ExtrasBasicBinding.inflate(layoutInflater, container, false)
        if (!GameMechanics.enableEditor)
            binding.buttonContainer.removeView(binding.buttonEditor)
        clock = SevenSegmentClock(resources.getDimension(R.dimen.sevensegment_display_height).toInt(),
                            requireActivity() as ExtrasActivity)
        updateClock()
        return binding.root
    }

    fun updateClock()
    {
        val clockView = binding.sevenSegmentClock
        clockView.setImageBitmap(clock.getDisplayBitmap())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val job = lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateClock()
                delay(450.milliseconds)
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
