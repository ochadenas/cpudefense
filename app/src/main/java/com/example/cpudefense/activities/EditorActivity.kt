@file:Suppress("SpellCheckingInspection")

package com.example.cpudefense.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.cpudefense.EditorView
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.Settings
import com.example.cpudefense.utils.Logger
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class EditorActivity : AppCompatActivity()
{
    var logger: Logger? = null
    lateinit var gameMechanics: GameMechanics
    lateinit var editorView: EditorView

    companion object;

    private var displayJob: Job? = null

    val settings = Settings()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        setContentView(R.layout.activity_main_game)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
    @OptIn(DelicateCoroutinesApi::class)
    override fun onResume()
    {
        super.onResume()
        loadSettings()
        if (settings.activateLogging && GameMechanics.enableLogging)
            logger = Logger(this, GameMechanics.logLevel)
        logger?.start()
        logger?.log("Entering editor activity.")
        setupEditorView()
        if (displayJob?.isActive != true)  // (!= true) is not the same as (false) here!
            displayJob = GlobalScope.launch { delay(GameActivity.effectsDelay.milliseconds); display(); }
        editorView.startNewCircuit()
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun display()
    /** Thread for refreshing the display on the screen.
     * The delay between two executions may vary. */
    {
        editorView.display()
        displayJob = GlobalScope.launch { delay(GameActivity.effectsDelay.milliseconds); display() }
    }

}