package com.example.cpudefense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.GestureDetectorCompat
import com.example.cpudefense.effects.Background
import com.example.cpudefense.effects.Effects

class GameView(context: Context, val theGame: Game):
    SurfaceView(context), SurfaceHolder.Callback,
    GestureDetector.OnGestureListener
{
    var canvas: Canvas? = null
    var theEffects: Effects? = null
    private var backgroundColour = Color.BLACK
    private var gestureDetector = GestureDetectorCompat(context, this)

    fun setup()
            /** called when ... TODO!
             *
             */
    {
        this.visibility = VISIBLE
        this.holder.addCallback(this)
        backgroundColour = theGame.resources.getColor(R.color.network_background)
        theEffects = Effects(theGame)
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

    inline fun scoreBoardHeight(h: Int): Int
    /** calculate score board size for a given screen size
    @param h total height of screen
     */
    {
        val scoreBoardHeight = (h*0.1).toInt()
        if (scoreBoardHeight < Game.minScoreBoardHeight)
            return Game.minScoreBoardHeight
        else if (scoreBoardHeight > Game.maxScoreBoardHeight)
            return Game.maxScoreBoardHeight
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
        theGame.viewport.setScreenSize(w, viewportHeight)
        theGame.scoreBoard.setSize(Rect(0, viewportHeight, w, viewportHeight+scoreBoardHeight(h)))
        theGame.speedControlPanel.setSize(Rect(0, 0, w, viewportHeight))
        theGame.intermezzo.setSize(Rect(0,0,w,h))
        theGame.marketplace.setSize(Rect(0,0,w,h))
        theGame.notification.setPositionOnScreen(w/2, h/2)
        theGame.background = Background(theGame)
        /* increase speed on larger screens */
        theGame.globalSpeedFactor = (h / 1024f)
        /* increase attacker size on larger screens */
        theGame.globalResolutionFactor = (h / 1024f)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        this.gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        p0?.let { theGame.onDown(it) }
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, dx: Float, dy: Float): Boolean {
        when (theGame.state.phase)
        {
            Game.GamePhase.MARKETPLACE -> theGame.marketplace.onScroll(p0, p1, dx, dy)
            else ->
            {
                theGame.viewport.addOffset(-dx, -dy)
                theGame.currentStage?.network?.recreateNetworkImage(theGame.viewport)
            }
        }


        return false
    }

    override fun onLongPress(p0: MotionEvent?) {
        theGame.currentStage?.network?.onLongPress(p0)
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }

    @Synchronized fun display()
    {
        val canvas = holder.lockCanvas()
        if (canvas != null)
        {
            canvas.drawColor(backgroundColour)

            theGame.display(canvas)
            theEffects?.display(canvas)

            holder.unlockCanvasAndPost(canvas)
        }
    }
}