@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.ViewGroup
import com.example.cpudefense.activities.EditorActivity
import com.example.cpudefense.editorElements.EditorPanel
import com.example.cpudefense.gameElements.CommonButtonPanel
import com.example.cpudefense.networkmap.Coord
import com.example.cpudefense.networkmap.Node
import com.example.cpudefense.utils.Logger
import com.example.cpudefense.utils.contains
import com.example.cpudefense.utils.scale
import kotlin.random.Random

class EditorView(context: Context):
    CommonView(context)
{
    val editorActivity = context as EditorActivity
    override val gameMechanics = editorActivity.gameMechanics
    val commonButtonPanel = CommonButtonPanel(this)
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
        scrollAllowed = false
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
        Rect(0, topMargin, w, h).let {
            editorPanel.setSize(it)
            commonButtonPanel.setSize(it)
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onDown(motionEvent: MotionEvent): Boolean {
        editorPanel.onDown(motionEvent)
        if (commonButtonPanel.onDown(motionEvent))
            background.prepareForEditor() // TODO
        gameMechanics.currentlyActiveStage?.network?.let {
            if (processClickOnNodes(it, motionEvent))
                return true
            return true
        }
        return false
    }

    /** called when the user makes a scrolling gesture.
     * @param p0 start position (first touch)
     * @param p1 current position
     * @param dx horizontal displacement
     * @param dy vertical displacement
     */
    override fun onScroll(p0: MotionEvent?, p1: MotionEvent, dx: Float, dy: Float): Boolean
    {
        val touchPosition = Pair(p1.x.toInt(), p1.y.toInt())

        // if a chip is active, move it
        val nodeToMove =  gameMechanics.currentlyActiveStage?.network?.nodes?.values?.firstOrNull { it.moveEnabled }
        nodeToMove?.let {
            val nodeRect = Rect(it.actualRect).scale(2.0f) // touch area is bigger than actual node, to make moving easier
            if (nodeRect.contains(touchPosition))
            {
                val displacement = Coord(dx / viewport.scaleX, dy / viewport.scaleY)
                val newCoord = viewport.screenToGrid(touchPosition).minus(displacement)
                it.placeOnGrid(viewport, newCoord.x, newCoord.y)
                it.calculateActualRect(viewport)
                it.theNetwork.recreateNetworkImage(false)
                return true
            }
        }

        // move the whole grid
        if (scrollAllowed) synchronized(scrollLock) {
            viewport.addOffset(-dx, -dy)
            gameMechanics.currentlyActiveStage?.network?.recreateNetworkImage(false)
        }

        return true
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
                commonButtonPanel.display(it)
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

    fun disableMoveForAllNodes()
    {
        gameMechanics.currentlyActiveStage?.network?.let { network ->
            network.nodes.values.firstOrNull { it.moveEnabled }?.let { node ->
                node.moveEnabled = false
                network.recreateNetworkImage(false)
            }
        }
    }

    fun enableMove(node: Node)
    {
        node.moveEnabled = false
        node.theNetwork.recreateNetworkImage(false)
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
        gameMechanics.currentlyActiveStage?.let {
            logger()?.log("Trying to add a new chip")
            var gridPosX = it.network.data.gridSizeX/2
            var gridPosY = it.network.data.gridSizeY/2
            val chip = it.createChip( gridPosX, gridPosY)
            disableMoveForAllNodes()
            chip.moveEnabled = true
            while (it.network.nodeTouches(chip as Node))
            {
                gridPosX += Random.nextInt(-10, 10)
                gridPosY += Random.nextInt(-10, 10)
                chip.placeOnGrid(viewport, gridPosX.toFloat(), gridPosY.toFloat())
            }
            it.network.recreateNetworkImage(false)
        }

    }

    override fun showReturnDialog()
    {
        editorActivity.finish()
    }

}