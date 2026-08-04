@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.GestureDetectorCompat
import com.example.cpudefense.activities.EditorActivity
import com.example.cpudefense.editorElements.EditorPanel
import com.example.cpudefense.effects.Effects
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.effects.Flipper
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("RedundantOverride")
class EditorView(context: Context):
    SurfaceView(context), SurfaceHolder.Callback,
    GestureDetector.OnGestureListener
{
    val editorActivity = context as EditorActivity
    val gameMechanics = editorActivity.gameMechanics
    var canvas: Canvas? = null
    var effects: Effects? = null
    /** whether the viewport can be moved by scrolling or scaled by pinching */
    var scrollAllowed = true

    /** lock used to synchronize drawing */
    private var displayLock = Any()
    /** lock used to synchronize scrolling */
    private var scrollLock = Any()

    private var backgroundColour = Color.BLACK
    private val gestureDetector = GestureDetectorCompat(context, this)

    val viewport = Viewport()
    val editorPanel = EditorPanel(this)
    /** list of all mover objects that are created for game elements */
    var movers = CopyOnWriteArrayList<Mover>()
    /** list of all fader objects that are created for game elements */
    var faders = CopyOnWriteArrayList<Fader>()
    /** list of all flipper objects that are created for game elements */
    var flippers = CopyOnWriteArrayList<Flipper>()

    /** text scale factor, based on ScaledDensity */
    var textScaleFactor = 1.0f
    /** general scale factor, based on Density */
    var scaleFactor = 1.0f
    /** space taken up by the top system bar */    val cpuImage: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.cpu)

    var topMargin = 0

    val menuIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.buttons_menu)
    val chipIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.buttons_chip)
    val moveIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.buttons_move)

    fun hasDefinedSize(): Boolean
    /** whether the game view and all its components know their size and can be used */
    {
        return (width > 0) && (height > 0)
    }

    /** called when the game view is created.
     * This is NOT the case when the user returns to the main menu
     *  and then continues the game.
     */
    fun setupView()
    {
        this.visibility = VISIBLE
        this.holder.addCallback(this)
        backgroundColour = context.resources.getColor(R.color.network_background)
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
    }

    @Suppress("UNUSED_VARIABLE", "unused")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int)
    /** function that is called to calculate height and width of this view. */
    {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int = widthSize
        val height: Int = heightSize
        setMeasuredDimension(width, height)
    }

    private fun scoreBoardHeight(h: Int): Int
    /** calculate score board size for a given screen size
    @param h total height of screen
     */
    {
        val scoreBoardHeight = (h*0.1).toInt()
        return scoreBoardHeight.coerceIn(GameView.minScoreBoardHeight, GameView.maxScoreBoardHeight)
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
        scaleFactor = 0.50f * resources.displayMetrics.density
        val viewportHeight = viewportHeight(h)
        viewport.determineScreenSize(w, viewportHeight, scaleFactor)
        editorPanel.setSize(Rect(0, 0, w, h))
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

        return false
    }

    override fun onShowPress(p0: MotionEvent) {
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, dx: Float, dy: Float): Boolean {

        return false
    }

    override fun onLongPress(motionEvent: MotionEvent)     {
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }


    fun display()
    {
        if (!hasDefinedSize())
            return

        synchronized(displayLock) {
            holder.lockCanvas()?.let()
            {
                editorPanel.display(it)
                holder.unlockCanvasAndPost(it)
            }
        }
    }

    private fun displayNetwork(canvas: Canvas)
    {
        canvas.let {
            gameMechanics.currentlyActiveStage?.network?.display(it, viewport)
        }
    }

}