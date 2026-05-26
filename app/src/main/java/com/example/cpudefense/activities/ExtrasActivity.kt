package com.example.cpudefense.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.cpudefense.R


class ExtrasActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        supportActionBar?.hide()
        setContentView(R.layout.activity_extras)
        /*
        findViewById<View>(android.R.id.content)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView, ::handleInsets)
        }
        */
        findViewById<ViewPager2>(R.id.viewPager).adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(p: Int) = when (p) {
                0 -> Page1Fragment()
                1 -> Page2Fragment()
                else -> Page3Fragment()
            }
        }
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        //val versionView: TextView = findViewById(R.id.about_version)
        //versionView.text = getString(R.string.about_version).format(info.versionName)

        val dots = listOf(findViewById<View>(R.id.led1), findViewById(R.id.led2), findViewById(R.id.led3))
        findViewById<ViewPager2>(R.id.viewPager).registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, v -> v.setBackgroundColor(if (i == position) resources.getColor(R.color.led_green)
                                                                   else resources.getColor(R.color.led_off)) }
            }
        })
    }

    fun handleInsets(view: View, windowInsets: WindowInsetsCompat): WindowInsetsCompat
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
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
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

class Page1Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.page1, container, false)
}

class Page2Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.page2, container, false)
}

class Page3Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.page3, container, false)
}
