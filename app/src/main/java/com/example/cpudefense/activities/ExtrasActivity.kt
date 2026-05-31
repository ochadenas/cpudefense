package com.example.cpudefense.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.example.cpudefense.R
import com.example.cpudefense.extras.SevenSegmentClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ExtrasActivity : AppCompatActivity()
{
    val aboutFragment = AboutFragment()
    val basicFragment = ExtrasBasicFragment()

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
                override fun getItemCount() = 3
                override fun createFragment(p: Int) = when (p) {
                    0 -> LevelStatisticsFragment()
                    2 -> aboutFragment
                    else -> basicFragment
                }
            }
            it.setCurrentItem(1, false)
        }
        val dots = listOf(findViewById<View>(R.id.led1), findViewById(R.id.led2), findViewById(R.id.led3))
        findViewById<ViewPager2>(R.id.viewPager).registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, v -> v.setBackgroundColor(if (i == position) resources.getColor(R.color.led_green)
                                                                   else resources.getColor(R.color.led_off)) }
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
            Toast.makeText(this, resources.getString(R.string.message_open_in_browser), Toast.LENGTH_SHORT).show()
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }


    fun releaseNotes(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val url = resources.getString(R.string.url_releasenotes_base).format(resources.getString(R.string.url_releasenotes_localized))
        Toast.makeText(this, resources.getString(R.string.message_open_in_browser), Toast.LENGTH_SHORT).show()
        val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }

    fun displayInfoDialog(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://github.com/ochadenas/cpudefense/wiki/Chip-Defense".toUri())
        try {
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }


}

class LevelStatisticsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.statistics, container, false)
}

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
        view?.let{
            val activity = requireActivity()
            val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_ACTIVITIES)
            val versionView: TextView = it.findViewById(R.id.fragment_about_version)
            versionView.text = getString(R.string.about_version).format(info.versionName)
        }
        return view
    }
}

class ExtrasBasicFragment : Fragment() {
    lateinit var contentsView: View
    lateinit var clock: SevenSegmentClock
    val isActive: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        contentsView = inflater.inflate(R.layout.extras_basic, container, false)
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
                delay(900)
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
