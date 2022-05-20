package com.example.cpudefense

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.icu.text.RelativeDateTimeFormatter
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.GestureDetectorCompat
import com.example.cpudefense.effects.Effects

class GameView(context: Context, val theGame: Game):
    SurfaceView(context), SurfaceHolder.Callback,
    GestureDetector.OnGestureListener
{
    var canvas: Canvas? = null
    var theEffects: Effects? = null
    private var gestureDetector = GestureDetectorCompat(context, this)

    fun setup()
            /** called when ... TODO!
             *
             */
    {
        this.visibility = VISIBLE
        this.holder.addCallback(this)
        theEffects = Effects(theGame)
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
        setSize(w, h)
    }

    private fun setSize(w: Int, h: Int)
    {
        /* determine dimensions of the different game areas */
        var scoreBoardHeight = (h*0.1).toInt()
        if (scoreBoardHeight < Game.Params.minScoreBoardHeight)
            scoreBoardHeight = Game.Params.minScoreBoardHeight
        else if (scoreBoardHeight > Game.Params.maxScoreBoardHeight)
            scoreBoardHeight = Game.Params.maxScoreBoardHeight
        var viewportHeight = h - scoreBoardHeight
        theGame.viewport.setSize(w, viewportHeight)
        theGame.scoreBoard.setSize(Rect(0, viewportHeight, w, viewportHeight+scoreBoardHeight))
        theGame.speedControlPanel.setSize(Rect(0, 0, w, viewportHeight))
        theGame.intermezzo.setSize(Rect(0,0,w,h))
        theGame.marketplace.setSize(Rect(0,0,w,h))
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (gestureDetector != null)
            this.gestureDetector!!.onTouchEvent(event)
        return true
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        if (p0 != null)
            theGame.onDown(p0)
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {
    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        if (theGame.state.phase == Game.GamePhase.MARKETPLACE)
            theGame.marketplace.onScroll(p0, p1, p2, p3)
        return false
    }

    override fun onLongPress(p0: MotionEvent?) {
        theGame.network?.onLongPress(p0)
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }

    @Synchronized fun display()
    {
        val canvas = holder.lockCanvas()
        if (canvas != null)
        {
            canvas.drawColor(theGame.resources.getColor(R.color.network_background) ?: Color.BLACK)


            theGame.display(canvas)
            theEffects?.display(canvas)

            holder.unlockCanvasAndPost(canvas)
        }
    }
}