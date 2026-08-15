@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.core.content.edit
import com.example.cpudefense.GameMechanics.GamePhase
import com.example.cpudefense.GameMechanics.LevelMode
import com.example.cpudefense.activities.GameActivity
import com.example.cpudefense.effects.Effects
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.gameElements.ControlButtonPanel
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.utils.Logger
import com.example.cpudefense.utils.displayTextCenteredInRect

@Suppress("RedundantOverride")
class GameView(context: Context):
    CommonView(context)
{
    val gameActivity = context as GameActivity
    override val gameMechanics = gameActivity.gameMechanics

    private val coinIconBlue: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    private val coinIconRed: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin_red)
    val playIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.play_active)
    val pauseIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_active)
    val fastIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.fast_active)
    val fastestIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.fastest_active)
    val returnIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cancel_active)
    val moveLockIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.move_lock)
    val moveUnlockIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.move_unlock)
    val zoomPlusIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom_plus)
    val zoomMinusIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.zoom_minus)

    /* game elements */

    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val controlButtonPanel = ControlButtonPanel(this)
    private val notification = ProgressNotification(this)


    /** called when the game view is created.
     * This is NOT the case when the user returns to the main menu
     *  and then continues the game.
     */
    override fun setupView()
    {
        super.setupView()
        loadGraphicalState()
        setComputerTypeface()
        effects = Effects(this)
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        // setSize(p2, p3)
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setComponentSize(w, h)
        background.setBackgroundDimensions(w, h, false)
    }

    override fun settings(): Settings
    {
        return gameActivity.settings
    }

    fun resetAtStartOfStage()
    {
        controlButtonPanel.resetButtons()
        scoreBoard.Lives()
        scoreBoard.recreateBitmap()
        viewport.reset()
        viewport.determineScreenSize(this.width, this.height, scaleFactor)
        gameMechanics.currentlyActiveStage?.network?.let {
            // viewport.determineGridSize(Coord(it.data.gridSizeX, it.data.gridSizeY))
            it.applyScale(viewport)
            it.recreateNetworkImage(false)
        }
        viewState = ViewState.NORMAL
        controlButtonPanel.setInfoLine(resources.getString(R.string.stage_number).format(gameMechanics.currentlyActiveStage?.numberAsString()))
    }

    private fun scoreBoardHeight(h: Int): Int
    /** calculate score board size for a given screen size
    @param h total height of screen
     */
    {
        val scoreBoardHeight = (h*0.1).toInt()
        return scoreBoardHeight.coerceIn(CommonView.minScoreBoardHeight, CommonView.maxScoreBoardHeight)
    }

    override fun viewportHeight(h: Int): Int
    /** calculate viewport size for a given screen size
    @param h total height of screen
     */
    {
        return h - scoreBoardHeight(h)
    }

    override fun setComponentSize(w: Int, h: Int)
    /** calculates and sets the size of the inner components of this view.
     * Also calculates the viewport dimensions.
     * Can be called multiple times. */
    {
        super.setComponentSize(w, h)
        saveGraphicalState()
        // determine dimensions of the different game areas
        val viewportHeight = viewportHeight(h)
        viewport.determineScreenSize(w, viewportHeight, scaleFactor)
        scoreBoard.setSize(Rect(0, viewportHeight, w, viewportHeight+scoreBoardHeight(h)))
        controlButtonPanel.setSize(Rect(0, topMargin, w, viewportHeight))
        intermezzo.setSize(Rect(0, 0, w, h))
        marketplace.setSize(Rect(0, topMargin, w, h))
        notification.setPositionOnScreen(w/2, h/2)
        effects?.setSize(Rect(0, 0, w, viewportHeight))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.gestureDetector.onTouchEvent(event)
        // this.scaleGestureDetector.onTouchEvent(event)
        this.performClick()
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onDown(motionEvent: MotionEvent): Boolean
    {
        when (gameMechanics.state.phase)
        {
            GamePhase.RUNNING -> {
                    if (controlButtonPanel.onDown(motionEvent))
                        return true
                    gameMechanics.currentlyActiveStage?.network?.let {
                        if (processClickOnNodes(it, motionEvent))
                            return true
                        for (obj in it.vehicles)
                            if ((obj as Attacker).onDown(motionEvent))
                                return true
                }
                return false
            }
            GamePhase.INTERMEZZO -> return intermezzo.onDown(motionEvent)
            GamePhase.MARKETPLACE -> return marketplace.onDown(motionEvent)
            GamePhase.PAUSED -> {
                if (controlButtonPanel.onDown(motionEvent))
                    return true
                gameMechanics.currentlyActiveStage?.network?.let {
                    if (processClickOnNodes(it, motionEvent))
                        return true
                }
            }
            else -> return false
        }
        return false
    }

    override fun logger(): Logger? {
        return gameActivity.logger
    }

    override fun onShowPress(p0: MotionEvent) {
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, dx: Float, dy: Float): Boolean {
        when (gameMechanics.state.phase)
        {
            GamePhase.MARKETPLACE -> marketplace.onScroll(p0, p1, dx, dy)
            GamePhase.INTERMEZZO -> intermezzo.onScroll(p0, p1, dx, dy)
            else ->
            {
                if (scrollAllowed) synchronized(scrollLock) {
                    viewport.addOffset(-dx, -dy)
                    gameMechanics.currentlyActiveStage?.network?.recreateNetworkImage(false)
                }
            }
        }
        viewState = ViewState.NORMAL
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent)     {
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    /**  execute all movers and faders */
    override fun updateEffects()
    {
        intermezzo.update()
        for (m in movers)
        {
            if (m?.type == Mover.Type.NONE)
                movers.remove(m)
            else
                m?.update()
        }
        for (m in faders)
        {
            if (m?.type == Fader.Type.NONE)
                faders.remove(m)
            else
                m?.update()
        }
        for (m in flippers)
        {
            if (m?.type == Flipper.Type.NONE)
                flippers.remove(m)
            else
                m?.update()
        }
        if (showAdditionalEffects())
            effects?.snow?.updateGraphicalEffects()
    }

    override fun display()
    {
        if (!hasDefinedSize())
            return
        val state = gameMechanics.state
        if (viewState == ViewState.CHANGING_SIZE)
            return
        synchronized(displayLock) {
            holder.lockCanvas()?.let()
            {
                if (state.phase == GamePhase.RUNNING || state.phase == GamePhase.PAUSED)
                    displayNetwork(it)
                if (showAdditionalEffects())
                    effects?.snow?.display(it)
                if (state.phase == GamePhase.PAUSED)
                    displayPauseIndicator(it)
                intermezzo.display(it, viewport)
                marketplace.display(it, viewport)
                notification.display(it)
                effects?.displayGraphicalEffects(it)
                holder.unlockCanvasAndPost(it)
            }
        }
    }

    private fun displayNetwork(canvas: Canvas)
    {
        canvas.let {
            gameMechanics.currentlyActiveStage?.network?.display(it, viewport)
            scoreBoard.display(it, viewport)
            controlButtonPanel.display(it)
        }
    }

    private fun displayPauseIndicator(canvas: Canvas)
    {
        canvas.let {
            val paint = Paint()
            paint.color = Color.WHITE
            paint.textSize = 72f
            paint.typeface = Typeface.DEFAULT_BOLD
            val rect = Rect(0, 0, viewport.viewportWidth, viewport.viewportHeight)
            rect.displayTextCenteredInRect(it, resources.getString(R.string.game_paused), paint)
        }
    }

    fun currentCoinBitmap(stage: Stage.Identifier = gameMechanics.currentStageIdent): Bitmap
    {
        return when (stage.mode())
        {
            LevelMode.BASIC -> coinIconBlue
            LevelMode.ENDLESS -> coinIconRed
        }
    }

    private fun saveGraphicalState()
    {
        gameActivity.getSharedPreferences(Persistency.filename_state, MODE_PRIVATE).edit {
            putFloat("SCALE_FACTOR", scaleFactor)
            putFloat("TEXT_SCALE_FACTOR", textScaleFactor)
        }
    }

    private fun loadGraphicalState()
    {
        val prefs = gameActivity.getSharedPreferences(Persistency.filename_state, MODE_PRIVATE)
        scaleFactor = prefs.getFloat("SCALE_FACTOR", 1.0f)
        textScaleFactor = prefs.getFloat("TEXT_SCALE_FACTOR", 1.0f)
    }

    private fun showAdditionalEffects(): Boolean
    {
        if (gameMechanics.currentStageIdent.mode() == LevelMode.BASIC && gameMechanics.currentStageIdent.number == GameMechanics.specialLevelNumber)
            return (gameMechanics.state.phase == GamePhase.RUNNING || gameMechanics.state.phase == GamePhase.PAUSED)
        else
            return false
    }

}