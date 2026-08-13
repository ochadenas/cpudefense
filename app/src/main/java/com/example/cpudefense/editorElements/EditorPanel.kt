package com.example.cpudefense.editorElements

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import com.example.cpudefense.EditorView
import com.example.cpudefense.CommonView
import com.example.cpudefense.editorElements.ControlButton.Type
import com.example.cpudefense.utils.setCenter

/** set of buttons that are used when editing */
class EditorPanel(var editorView: EditorView)
{
    private val gameMechanics = editorView.gameMechanics
    private var buttonMenu = ControlButton(editorView, editorView.gameMechanics, Type.MENU, this)
    private var buttonNewChip = ControlButton(editorView, editorView.gameMechanics, Type.NEW_CHIP, this)
    private var buttonMoveChip = ControlButton(editorView, editorView.gameMechanics, Type.MOVE_CHIP, this)
    private var buttons = mutableListOf( buttonMenu, buttonNewChip, buttonMoveChip)
    /** area that holds the  buttons */
    private var areaBottom = Rect(0,0,0,0)
    /** the size of the control buttons in pixels, with density factor applied */
    var actualButtonSize: Int = 0

    fun setSize(parentArea: Rect)
    {
        actualButtonSize = CommonView.speedControlButtonSize * editorView.resources.displayMetrics.density.toInt()
        val margin = actualButtonSize / 5   // space between the buttons
        buttons.forEach {it.setSize(actualButtonSize)}
        areaBottom.right = parentArea.right - margin
        areaBottom.bottom = parentArea.bottom - margin
        areaBottom.left = areaBottom.right - (actualButtonSize + margin) * buttons.size
        areaBottom.top = areaBottom.bottom - actualButtonSize
        buttonMenu.area.setCenter(areaBottom.left + actualButtonSize / 2, areaBottom.centerY())
        buttonNewChip.area.setCenter(areaBottom.right - actualButtonSize / 2, areaBottom.centerY())
        buttonMoveChip.area.setCenter(areaBottom.left - actualButtonSize / 2 - margin, areaBottom.centerY())
    }

    fun onDown(p0: MotionEvent): Boolean {
        buttons.forEach { if (it.onDown(p0)) return true }
        return false
    }

    fun display(canvas: Canvas) {
        if (areaBottom.left == 0)
            return
        buttons.forEach { it.display(canvas)}
    }
}