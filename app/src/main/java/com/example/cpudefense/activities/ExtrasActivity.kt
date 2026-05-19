package com.example.cpudefense.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cpudefense.R


class ExtrasActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        supportActionBar?.hide()
        setContentView(R.layout.activity_extras)
        findViewById<View>(android.R.id.content)?.let { rootView ->
            ViewCompat.setOnApplyWindowInsetsListener(rootView, ::handleInsets)
        }
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        val versionView: TextView = findViewById(R.id.about_version)
        versionView.text = getString(R.string.about_version).format(info.versionName)
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
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://github.com/ochadenas/cpudefense/wiki/Chip-Defense".toUri())
        try {
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }


    fun displayInfo(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://github.com/ochadenas/cpudefense/wiki/Chip-Defense".toUri())
        try {
            startActivity(browserIntent)
        }
        catch (_: Exception) {}  // come here if no external app can handle the request
    }


}