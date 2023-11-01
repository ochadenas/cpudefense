package com.example.cpudefense

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat


class AboutActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        findViewById<TextView>(R.id.about_text_view)?.movementMethod = ScrollingMovementMethod()
    }

    fun dismiss(@Suppress("UNUSED_PARAMETER") v: View)
    {
        finish()
    }


}