@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewGroup
import com.example.cpudefense.activities.EditorActivity
import com.example.cpudefense.editorElements.EditorPanel
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.utils.Logger

class EditorView(context: Context):
    CommonView(context)
{
    val editorActivity = context as EditorActivity
    override val gameMechanics = editorActivity.gameMechanics
    val editorPanel = EditorPanel(this)

    val menuIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.buttons_menu)
    val chipIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.buttons_chip)
    val moveIcon: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.buttons_move)

    override fun surfaceCreated(p0: SurfaceHolder) {
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewport.determineScreenSize(this.width, this.height, scaleFactor)
        setComponentSize(w, h)
        background.prepareForEditor()
    }

    override fun settings(): Settings
    {
        return editorActivity.settings
    }

    override fun setComponentSize(w: Int, h: Int)
    /** calculates and sets the size of the inner components of this view.
     * Also calculates the viewport dimensions.
     * Can be called multiple times. */
    {
        super.setComponentSize(w, h)
        setComputerTypeface()
        editorPanel.setSize(Rect(0, 0, w, h))
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        editorPanel.onDown(motionEvent)
        gameMechanics.currentlyActiveStage?.network?.let {
            if (processClickOnNodes(it, motionEvent))
                return true
            return true
        }
        return false
    }

    override fun display()
    {
        if (!hasDefinedSize())
            return

        synchronized(super.displayLock) {
            holder.lockCanvas()?.let()
            {
                gameMechanics.currentlyActiveStage?.network?.display(it, viewport)
                editorPanel.display(it)
                holder.unlockCanvasAndPost(it)
            }
        }
    }

    fun showMenu()
    {
        val dialog = Dialog(editorActivity)
        dialog.setContentView(R.layout.layout_editor_menu)
        dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)
        dialog.show()
    }

    fun startNewCircuit()
    {
        logger()?.log("Starting new circuit.")
        gameMechanics.currentlyActiveStage = Stage(gameMechanics, this)
            .also{ it.initializeNetwork(50, 50)
                it.data.ident = Stage.Identifier(GameMechanics.SERIES_USER, 1) // TODO: attribute a number
                gameMechanics.currentStageIdent = it.data.ident
            }
    }

    override fun logger(): Logger? {
        return editorActivity.logger
    }

    fun addChip()
    {
        gameMechanics.currentlyActiveStage?.createChip(10, 10, 1)

    }

}