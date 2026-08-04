@file:Suppress("SpellCheckingInspection")

package com.example.cpudefense.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.cpudefense.EditorView
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.GameMechanics.Params.SERIES_NORMAL
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.Settings
import com.example.cpudefense.Stage.Identifier
import com.example.cpudefense.utils.Logger

class EditorActivity : AppCompatActivity()
{
    var logger: Logger? = null
    lateinit var gameMechanics: GameMechanics
    lateinit var editorView: EditorView

    companion object;

    val settings = Settings()
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContentView(R.layout.activity_main_game)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (intent.getBooleanExtra("ACTIVATE_LOGGING", false) && GameMechanics.enableLogging)
            logger = Logger(this, GameMechanics.logLevel)
        logger?.start()
        gameMechanics = GameMechanics()
        editorView = EditorView(this)
        // after this, onResume() is called by the system
    }

    /** method executed when the user presses the system's "back" button,
     *  but also when they navigate to another app
     */
    override fun onPause()
    {
        logger?.log("Pausing Editor Activity")
        super.onPause()
    }

    /** function that gets called in any case, regardless of whether
     * a new game is started or the user just navigates back to the app.
     */
    override fun onResume()
    {
        super.onResume()
        Toast.makeText(this, resources.getString(R.string.toast_loading), Toast.LENGTH_SHORT).show()
        loadSettings()
        setupEditorView()
        logger?.log("Entering game activity, game state is %s".format(gameMechanics.state.toString()))

        // determine what to do: resume, restart, or play next level
        var restartGame = intent.getBooleanExtra("RESET_PROGRESS", false)
        var restartEndless = intent.getBooleanExtra("RESET_ENDLESS", false)
        val restoreGame = intent.getBooleanExtra("LOAD_PROGRESS", false)

        var startOnLevel = when
        {
            restartGame -> Identifier.startOfNewGame
            restoreGame -> Identifier.startOfNewGame // this will be overwritten later
            restartEndless -> Identifier.startOfEndless
            else -> Identifier(
                    series = intent.getIntExtra("START_ON_SERIES", SERIES_NORMAL),
                    number = intent.getIntExtra("START_ON_STAGE", 1)
            )
        }

    }

    override fun onStop() {
        logger?.log("Stopping Editor Activity")
        super.onStop()
    }

    override fun onDestroy() {
        logger?.log("Ending Editor Activity")
        logger?.stop()
        super.onDestroy()
    }

    /** handles the width of the system status bar (top and bottom) and applies
     * margins in order to avoid overlapping of game elements
     */
    fun handleInsets(view: View, windowInsets: WindowInsetsCompat): WindowInsetsCompat
    {
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams>
        {
            topMargin = insets.top
            leftMargin = insets.left
            bottomMargin = insets.bottom
            rightMargin = insets.right
        }
        (view as EditorView).topMargin = insets.top + 2 // remember this value
        return WindowInsetsCompat.CONSUMED
    }

    private fun setupEditorView()
    /** creates the game view including all game components */
    {
        if (editorView.parent == null)
        {
            val parentView: FrameLayout? = findViewById(R.id.gameFrameLayout)
            parentView?.addView(editorView)
            ViewCompat.setOnApplyWindowInsetsListener(editorView, ::handleInsets)
        }
        editorView.setupView()
    }

    private fun loadSettings()
            /** load global configuration and debug settings from preferences */
    {
        val prefs = getSharedPreferences(Persistency.filename_settings, MODE_PRIVATE)
        settings.loadFromFile(prefs)
    }

    private fun display()
    /** Thread for refreshing the display on the screen.
     * The delay between two executions may vary. */
    {
        editorView.display()
    }

}