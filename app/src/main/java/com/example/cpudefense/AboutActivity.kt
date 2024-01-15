package com.example.cpudefense

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat


class AboutActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val textView = findViewById<TextView>(R.id.about_text_view)
        textView.movementMethod = ScrollingMovementMethod()
        textView.movementMethod = LinkMovementMethod.getInstance()
        val info = packageManager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        val versionView: TextView = findViewById<TextView>(R.id.about_version)
        versionView.text = getString(R.string.about_version).format(info.versionName)
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }

    fun wiki(@Suppress("UNUSED_PARAMETER") v: View)
    {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ochadenas/cpudefense/wiki"))
        try {
            startActivity(browserIntent)
        }
        catch (exception: Exception) {}  // come here if no external app can handle the request
    }


}