package com.example.cpudefense

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding


class AboutActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val textView = findViewById<TextView>(R.id.about_text_view)
        textView.movementMethod = ScrollingMovementMethod()
        textView.movementMethod = LinkMovementMethod.getInstance()
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        val versionView: TextView = findViewById(R.id.about_version)
        versionView.text = getString(R.string.about_version).format(info.versionName)
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }

    fun wiki(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ochadenas/cpudefense/wiki/Chip-Defense"))
        try {
            startActivity(browserIntent)
        }
        catch (exception: Exception) {}  // come here if no external app can handle the request
    }

    fun releaseNotes(@Suppress("UNUSED_PARAMETER") v: View)
    {
        try {

            val contentView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.contents_section)
            contentView.removeAllViews()
            val textView = TextView(this)
            textView.text = getString(R.string.ZZ_release_notes)
            textView.setPadding(8)
            // textView.typeface = ResourcesCompat.getFont(this, R.font.ubuntu_mono_bold)
            textView.setTextColor(Color.WHITE)
            textView.textSize = 12f
            textView.movementMethod = ScrollingMovementMethod()
            contentView.addView(textView)
        }
        catch (exception: Exception) {}
    }

}