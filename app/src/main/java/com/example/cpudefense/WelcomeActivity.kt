package com.example.cpudefense

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.appcompat.app.AlertDialog

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }

    fun continueGame(v: View)
    {
        val intent = Intent(this, MainGameActivity::class.java)
        intent.putExtra("CONTINUE_GAME", true)
        startActivity(intent)
    }

    fun startLevelSelection(v: View)
    {
        val intent = Intent(this, LevelSelectActivity::class.java)
        startActivity(intent)
    }

    fun startNewGame(v: View)
    {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Really start new game? This will reset all your progress.")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    val intent = Intent(this, MainGameActivity::class.java)
                    intent.putExtra("RESET_PROGRESS", true)
                    intent.putExtra("START_ON_STAGE", 1)
                    intent.putExtra("CONTINUE_GAME", false)
                    startActivity(intent)
                }
                .setNegativeButton("No") { dialog, id -> dialog.dismiss() }
        val alert = builder.create()
        alert.show()
    }


}