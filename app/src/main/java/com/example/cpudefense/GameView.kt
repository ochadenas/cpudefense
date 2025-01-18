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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GestureDetectorCompat
import com.example.cpudefense.GameMechanics.GamePhase
import com.example.cpudefense.GameMechanics.LevelMode
import com.example.cpudefense.activities.GameActivity
import com.example.cpudefense.effects.Background
import com.example.cpudefense.effects.Effects
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.gameElements.SpeedControl
import com.example.cpudefense.networkmap.Coord
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import java.util.concurrent.CopyOnWriteArrayList

class GameView(context: Context):
    SurfaceView(context), SurfaceHolder.Callback,
    GestureDetector.OnGestureListener
{
    @Suppress("ConstPropertyName")
    companion object {
        // default sizes for graphical game elements.
        const val scoreTextSize = 36f
        const val scoreHeaderSize = 18f
        const val chipTextSize = 20f
        const val computerTextSize = 26f
        const val notificationTextSize = 22f
        const val instructionTextSize = 25f
        const val biographyTextSize = 20f
        const val heroCardNameSize = 18f
        const val heroCardTextSize = 14f
        const val purchaseButtonTextSize = 20f
        const val coinsAmountTextSize = 24f

        const val coinSizeOnScoreboard = 48
        const val coinSizeOnScreen = 16
        const val cardWidth = 220
        const val cardHeight = cardWidth * 1.41
        const val cardPictureSize = cardWidth * 2 / 3
        /** horizontal size of LEDs, can be smaller if there is too little space */
        const val preferredSizeOfLED = 20

        val chipSize = Coord(6,3)
        const val viewportMargin = 10
        const val minScoreBoardHeight = 100
        const val maxScoreBoardHeight = 320
        const val speedControlButtonSize = 48
        const val levelSnapshotIconSize = 120
    }

    val gameActivity = context as GameActivity
    val gameMechanics = gameActivity.gameMechanics
    var canvas: Canvas? = null
    var effects: Effects? = null
    var scrollAllowed = true // whether the viewport can be moved by scrolling
    private var backgroundColour = Color.BLACK
    private var gestureDetector = GestureDetectorCompat(context, this)

    /** font for displaying "computer messages" */
    lateinit var monoTypeface: Typeface
    lateinit var boldTypeface: Typeface

    private val coinIconBlue: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin)
    private val coinIconRed: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cryptocoin_red)
    val cpuImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cpu)
    val playIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.play_active)
    val pauseIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.pause_active)
    val fastIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.fast_active)
    val returnIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cancel_active)
    val moveLockIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.move_lock)
    val moveUnlockIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.move_unlock)
    val hpBackgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.hp_key)

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
    private val notification = ProgressNotification(this)

    /** text scale factor, based on ScaledDensity */
    var textScaleFactor = 1.0f
    /** general scale factor, based on ScaledDensity */
    var scaleFactor = 1.0f

    fun isInitialized(): Boolean
    /** whether the game view and all its components know their size and can be used */
    {
        return (width > 0) && (height > 0)
    }

    fun setupView()
            /** called when the game view is created.
             * This is NOT the case when the user returns to the main menu
             *  and then continues the game.
             */
    {
        this.visibility = VISIBLE
        this.holder.addCallback(this)
        backgroundColour = context.resources.getColor(R.color.network_background)
        loadGraphicalState()
        setComputerTypeface()
        effects = Effects(this)
    }


    private fun setComputerTypeface()
    {
        try
        {
            monoTypeface = ResourcesCompat.getFont(context, R.font.ubuntu_mono) ?: Typeface.MONOSPACE
            boldTypeface = ResourcesCompat.getFont(context, R.font.ubuntu_mono_bold) ?: Typeface.MONOSPACE
        }
        catch (ex: Exception)
        {
            monoTypeface = Typeface.MONOSPACE
            boldTypeface = Typeface.MONOSPACE
        }
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

    @Suppress("UNUSED_VARIABLE")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
            /** function that is called to calculate height and width of this view.
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

    fun resetAtStartOfStage()
    {
        speedControlPanel.resetButtons()
        scoreBoard.Lives()
        scoreBoard.recreateBitmap()
        viewport.reset()
        viewport.setScreenSize(this.width, this.height)
        gameMechanics.currentlyActiveStage?.network?.recreateNetworkImage(false)
    }

    private fun scoreBoardHeight(h: Int): Int
    /** calculate score board size for a given screen size
    @param h total height of screen
     */
    {
        val scoreBoardHeight = (h*0.1).toInt()
        if (scoreBoardHeight < minScoreBoardHeight)
            return minScoreBoardHeight
        else if (scoreBoardHeight > maxScoreBoardHeight)
            return maxScoreBoardHeight
        else
            return scoreBoardHeight
    }

    private fun viewportHeight(h: Int): Int
    /** calculate viewport size for a given screen size
    @param h total height of screen
     */
    {
        return h - scoreBoardHeight(h)
    }

    private fun setComponentSize(w: Int, h: Int)
    /** calculates and sets the size of the inner components of this view.
     * Also calculates the viewport dimensions.
     * Can be called multiple times. */
    {
        // adjust text sizes and scaling factor
        textScaleFactor = 0.70f * resources.displayMetrics.scaledDensity
        scaleFactor = 0.50f * resources.displayMetrics.scaledDensity
        saveGraphicalState()
        // determine dimensions of the different game areas
        val viewportHeight = viewportHeight(h)
        viewport.setScreenSize(w, viewportHeight)
        scoreBoard.setSize(Rect(0, viewportHeight, w, viewportHeight+scoreBoardHeight(h)))
        speedControlPanel.setSize(Rect(0, 0, w, viewportHeight))
        intermezzo.setSize(Rect(0, 0, w, h))
        marketplace.setSize(Rect(0, 0, w, h))
        notification.setPositionOnScreen(w/2, h/2)
        effects?.setSize(Rect(0, 0, w, viewportHeight))

        /* increase attacker size on larger screens */
        // theGame.globalResolutionFactorX = (w / )
        // gameMechanics.resources.displayMetrics.scaledDensity = (h / 1024f) * resources.displayMetrics

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

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, dx: Float, dy: Float): Boolean {
        when (gameMechanics.state.phase)
        {
            GamePhase.MARKETPLACE -> marketplace.onScroll(p0, p1, dx, dy)
            GamePhase.INTERMEZZO -> intermezzo.onScroll(p0, p1, dx, dy)
            else ->
            {
                if (scrollAllowed) {
                    viewport.addOffset(-dx, -dy)
                    gameMechanics.currentlyActiveStage?.network?.recreateNetworkImage(false)
                }
            }
        }
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
        p0.let {
            when (gameMechanics.state.phase) {
                GamePhase.RUNNING -> gameMechanics.currentlyActiveStage?.network?.onLongPress(p0)
                else -> {}
            }
        }
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
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
        if (showAdditionalEffects())
            effects?.snow?.updateGraphicalEffects()
    }

    @Synchronized fun display()
    {
        if (!isInitialized())
            return
        val state = gameMechanics.state
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

    private fun displayNetwork(canvas: Canvas)
    {
        canvas.let {
            gameMechanics.currentlyActiveStage?.network?.display(it, viewport)
            scoreBoard.display(it, viewport)
            speedControlPanel.display(it)
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
        val editor = gameActivity.getSharedPreferences(Persistency.filename_state, MODE_PRIVATE).edit()
        editor.putFloat("SCALE_FACTOR", scaleFactor)
        editor.putFloat("TEXT_SCALE_FACTOR", textScaleFactor)
        editor.apply()
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