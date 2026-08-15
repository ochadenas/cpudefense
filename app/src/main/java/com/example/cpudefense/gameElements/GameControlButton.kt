package com.example.cpudefense.gameElements

import android.view.MotionEvent
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.GameView

class GameControlButton(val gameView: GameView, gameMechanics: GameMechanics,
                        type: Type = Type.PAUSE, private val panel: GameControlButtonPanel)
    : CommonControlButton(gameView, gameMechanics, type), Fadable
{
    override fun setSize(size: Int)
    {
        super.setSize(size)
    }

    override fun fadeDone(type: Fader.Type) {
    }

    override fun setOpacity(opacity: Float) {
        alpha = (opacity * 255).toInt()
    }

    override fun onDown(p0: MotionEvent): Boolean {
        if (super.onDown(p0))
            return true
        if (area.contains(p0.x.toInt(), p0.y.toInt()))
        {
            when (type)
            {
                Type.PAUSE -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.NORMAL)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.PAUSED)
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.NORMAL -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.NORMAL)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.RUNNING)
                    panel.resetButtons()
                }
                Type.FAST -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.FAST)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.RUNNING)
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.FASTEST -> {
                    gameView.gameActivity.setGameSpeed(GameMechanics.GameSpeed.MAX)
                    gameView.gameActivity.changeToGamePhase(GameMechanics.GamePhase.RUNNING)
                    panel.resetButtons()
                    type = Type.NORMAL
                }
                Type.RETURN -> {
                    gameView.gameActivity.showReturnDialog()
                }
                Type.LOCK -> {
                    gameView.scrollAllowed = true
                    type = Type.UNLOCK
                }
                Type.UNLOCK -> {
                    gameView.scrollAllowed = false
                    type = Type.LOCK
                }
                else -> { return false }
            }
            return true
        }
        else
            return false
    }

}