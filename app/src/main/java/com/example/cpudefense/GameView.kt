package com.example.cpudefense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.GestureDetectorCompat
import com.example.cpudefense.GameMechanics.GamePhase
import com.example.cpudefense.effects.Background
import com.example.cpudefense.effects.Effects
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.gameElements.SpeedControl
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import java.util.concurrent.CopyOnWriteArrayList

class GameView(context: Context, val gameMechanics: GameMechanics):
    SurfaceView(context), SurfaceHolder.Callback,
    GestureDetector.OnGestureListener
{
    var canvas: Canvas? = null
    var theEffects: Effects? = null
    var backgroundColour = Color.BLACK
    var scrollAllowed = true // whether the viewport can be moved by scrolling
    private var gestureDetector = GestureDetectorCompat(context, this)

    /** font for displaying "computer messages" */
    lateinit var monoTypeface: Typeface
    lateinit var boldTypeface: Typeface

    /* game elements */
    val viewport = Viewport()
    var background = Background(this)
    var intermezzo = Intermezzo(this)
    var marketplace = Marketplace(this)
    val scoreBoard = ScoreBoard(this)
    val speedControlPanel = SpeedControl(this)
    /** list of all mover objects that are created for game elements */
    var movers = CopyOnWriteArrayList<Mover>()
    /** list of all fader objects that are created for game elements */
    var faders = CopyOnWriteArrayList<Fader>()
    /** list of all flipper objects that are created for game elements */
    var flippers = CopyOnWriteArrayList<Flipper>()
    val notification = ProgressNotification(this)

    fun setup()
            /** called when the game view is created.
             * This is NOT the case when the user returns to the main menu
             *  and then continues the game.
             */
    {
        this.visibility = VISIBLE
        this.holder.addCallback(this)
        backgroundColour = context.resources.getColor(R.color.network_background)
        theEffects = Effects(gameMechanics)
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        if (this.width > 0 && this.height > 0)
            setSize(width, height)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        // setSize(p2, p3)
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setSize(w, h)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
            /** function that is called to calculate height and width of this view.
             * At this point, we also calculate the dimensions of all internal elements.
             */
    {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = widthSize
        val height: Int = heightSize
        setMeasuredDimension(width, height)
    }

    inline fun scoreBoardHeight(h: Int): Int
    /** calculate score board size for a given screen size
    @param h total height of screen
     */
    {
        val scoreBoardHeight = (h*0.1).toInt()
        if (scoreBoardHeight < GameMechanics.minScoreBoardHeight)
            return GameMechanics.minScoreBoardHeight
        else if (scoreBoardHeight > GameMechanics.maxScoreBoardHeight)
            return GameMechanics.maxScoreBoardHeight
        else
            return scoreBoardHeight
    }

    inline fun viewportHeight(h: Int): Int
    /** calculate viewport size for a given screen size
    @param h total height of screen
     */
    {
        return h - scoreBoardHeight(h)
    }

    private fun setSize(w: Int, h: Int)
    {
        /* determine dimensions of the different game areas */
        val viewportHeight = viewportHeight(h)
        viewport.setScreenSize(w, viewportHeight)
        scoreBoard.setSize(Rect(0, viewportHeight, w, viewportHeight+scoreBoardHeight(h)))
        speedControlPanel.setSize(Rect(0, 0, w, viewportHeight))
        intermezzo.setSize(Rect(0, 0, w, h))
        marketplace.setSize(Rect(0, 0, w, h))
        notification.setPositionOnScreen(w/2, h/2)
        /* increase attacker size on larger screens */
        // theGame.globalResolutionFactorX = (w / )
        gameMechanics.resources.displayMetrics.scaledDensity = (h / 1024f)
        resources.displayMetrics
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        this.gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        when (gameMechanics.state.phase)
        {
            GamePhase.RUNNING ->
            {
                if (speedControlPanel.onDown(motionEvent))
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
            GamePhase.INTERMEZZO ->
                return intermezzo.onDown(motionEvent)
            GamePhase.MARKETPLACE ->
                return marketplace.onDown(motionEvent)
            GamePhase.PAUSED ->
            {
                if (speedControlPanel.onDown(motionEvent))
                    return true
                gameMechanics.currentlyActiveStage?.network?.let {
                    if (processClickOnNodes(it, motionEvent))
                        return true
                }
            }
            else ->
                return false
        }
        return false
    }

    private fun processClickOnNodes(network: Network, p0: MotionEvent): Boolean
    {
        /* first, check if the click is inside one of the upgrade boxes
        * of _any_ node */
        for (obj in network.nodes.values) {
            val chip = obj as Chip
            for (upgrade in chip.upgradePossibilities)
                if (upgrade.onDown(p0)) {
                    chip.upgradePossibilities.clear()
                    return true
                }
            /* if we come here, then the click was not on an update. */
            if (chip.actualRect?.contains(p0.x.toInt(), p0.y.toInt()) == false)
                chip.upgradePossibilities.clear()  // clear update boxes of other chips
        }
        /* check the nodes themselves */
        for (obj in network.nodes.values)
            if (obj.onDown(p0))
                return true
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, dx: Float, dy: Float): Boolean {
        when (gameMechanics.state.phase)
        {
            GameMechanics.GamePhase.MARKETPLACE -> marketplace.onScroll(p0, p1, dx, dy)
            else ->
            {
                if (scrollAllowed) {
                    viewport.addOffset(-dx, -dy)
                    gameMechanics.currentlyActiveStage?.network?.recreateNetworkImage(viewport)
                }
            }
        }
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
        p0.let {
            when (gameMechanics.state.phase) {
                GameMechanics.GamePhase.RUNNING -> gameMechanics.currentlyActiveStage?.network?.onLongPress(p0)
                GameMechanics.GamePhase.MARKETPLACE -> marketplace.onLongPress(p0)
                else -> {}
            }
        }
    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    fun updateEffects()
            /**  execute all movers and faders */
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
    }

    @Synchronized fun display()
    {
        val state = gameMechanics.state
        holder.lockCanvas()?.let()
        {
            background.display(it)
            if (state.phase == GamePhase.RUNNING || state.phase == GamePhase.PAUSED)
            {
                gameMechanics.currentlyActiveStage?.network?.display(it, viewport)
                scoreBoard.display(it, viewport)
                speedControlPanel.display(it)
            }
            if (state.phase == GamePhase.PAUSED)
            {
                val paint = Paint()
                paint.color = Color.WHITE
                paint.textSize = 72f
                paint.typeface = Typeface.DEFAULT_BOLD
                val rect = Rect(0, 0, viewport.viewportWidth, viewport.viewportHeight)
                rect.displayTextCenteredInRect(it, resources.getString(R.string.game_paused), paint)
            }
            marketplace.display(it, viewport)
            notification.display(it)
            theEffects?.display(it)
            holder.unlockCanvasAndPost(it)
        }
    }
}