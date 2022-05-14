package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Node
import com.example.cpudefense.networkmap.Viewport
import java.util.concurrent.CopyOnWriteArrayList

open class Chip(val network: Network, gridX: Int, gridY: Int): Node(network, gridX.toFloat(), gridY.toFloat())
{
    enum class ChipType { EMPTY, SUB, SHIFT, AND, ENTRY, CPU}
    enum class ChipUpgrades { POWERUP, SUB, SHIFT, AND }

    data class Data(
        var type: ChipType = ChipType.EMPTY,
        var power: Int = 0,
        var cooldown: Int = 20,
        var value: Int = 0,
        var node: Node.Data,
        var color: Int = Color.WHITE,
        var glowColor: Int = Color.WHITE
    )

    var chipData = Data(
        node = super.data
    )

    open var bitmap: Bitmap? = null
    private var widthOnScreen: Int = 0
    private var heightOnScreen = 0

    val resources = network.theGame.resources

    private var cooldownTimer = 0
    private var upgradePossibilities = CopyOnWriteArrayList<ChipUpgrade>()

    init {
        actualRect = Rect()
        data.range = 2.0f
        when (chipData.type)
        {
            ChipType.SUB -> {
                val modifier: Float = network.theGame.gameUpgrades[Upgrade.Type.INCREASE_CHIP_SUB_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (20f / modifier).toInt()
            }
            ChipType.SHIFT -> {
                val modifier: Float = network.theGame.gameUpgrades[Upgrade.Type.INCREASE_CHIP_SHIFT_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (32f / modifier).toInt()
            }
        }
    }

    fun setIdent(ident: Int)
    {
        data.ident = ident
    }

    fun setType(chipType: ChipType)
    {
        chipData.type = chipType
        when (chipType)
        {
            ChipType.SUB -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_dec_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_dec_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SUB] ?: 10
            }
            ChipType.SHIFT -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_shr_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_shr_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SHIFT] ?: 10
            }
            else -> {}
        }
    }

    fun addPower(amount: Int)
    {
        chipData.power += amount
        bitmap = null
    }

    override fun update() {
        super.update()
        if (cooldownTimer>0) {
            cooldownTimer--
            return
        }
        val attackers = attackersInRange()
        if (attackers.isNotEmpty())
            shootAt(attackersInRange()[0])
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (chipData.type == ChipType.ENTRY)
            return super.display(canvas, viewport)  // TODO. temporary, just as long as ENTRY does not have its own display method

        /* calculate size */
        /* this is put here because the viewport / zoom factor may change.
        However, it may be possible to remove this from display()
         */
        val sizeOnScreen = theNetwork?.distanceBetweenGridPoints()
        if (sizeOnScreen != null) {
            widthOnScreen = sizeOnScreen.first * Game.chipSize.x.toInt()
            heightOnScreen = sizeOnScreen.second * Game.chipSize.y.toInt()
            actualRect = Rect(0, 0, widthOnScreen, heightOnScreen)
        }
        actualRect.setCenter(viewport.gridToScreen(posOnGrid))

        val paint = Paint()
        /* draw background */
        paint.color = resources.getColor(R.color.chips_background)
        paint.style = Paint.Style.FILL
        canvas.drawRect(actualRect, paint)
        if (cooldownTimer>0)
        {
            paint.color = chipData.glowColor
            paint.alpha = (cooldownTimer*255)/chipData.cooldown
            canvas.drawRect(actualRect, paint)
        }

        /* draw outline */
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(actualRect, paint)

        /* display a line to all vehicles in range */
        if (Game.drawLinesFromChip) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.WHITE
            paint.strokeWidth = 4.0f
            for (vehicle in distanceToVehicle.keys.filter { attackerInRange(it as Attacker) })
                canvas.drawLine(
                    actualRect.centerX().toFloat(), actualRect.centerY().toFloat(),
                    (viewport.gridToScreen(vehicle.posOnGrid!!)).first.toFloat(),
                    (viewport.gridToScreen(vehicle.posOnGrid!!)).second.toFloat(), paint
                )
        }

        if (bitmap == null)
            bitmap = createBitmapForType()
        if (bitmap != null)
            canvas.drawBitmap(bitmap!!, null, actualRect, paint)

        /* if applicable, show the different upgrade possibilities */
        for (upgrade in upgradePossibilities)
            upgrade.display(canvas)
    }

    fun attackerInRange(attacker: Attacker): Boolean
    {
        var dist: Float? = distanceTo(attacker)
        if (dist != null) {
            return dist <= data.range
        } else return false
    }

    fun attackersInRange(): List<Attacker>
    {
        val vehicles = distanceToVehicle.keys.filter { attackerInRange(it as Attacker) }.map { it as Attacker }
        return vehicles
    }

    fun shootAt(attacker: Attacker?)
    {
        if (chipData.type == ChipType.EMPTY)
            return
        cooldownTimer = chipData.cooldown
        var kill = attacker?.onShot(chipData.type, chipData.power)
        if (kill == true)
            attacker?.remove()
    }

    private fun createBitmapForType(): Bitmap?
    {
        when (chipData.type)
        {
            ChipType.SUB -> return createBitmap("SUB %d".format(chipData.power))
            ChipType.SHIFT -> return createBitmap("SHR %d".format(chipData.power))
            else -> return null
        }
    }

    private fun createBitmap(text: String): Bitmap?
    {
        if (actualRect.width() == 0 || actualRect.height() == 0)
            return null

        val bitmap = Bitmap.createBitmap(actualRect.width(), actualRect.height(), Bitmap.Config.ARGB_8888)
        var rect = Rect(0, 0, bitmap.width, bitmap.height)
        val canvas = Canvas(bitmap)
        var paint = Paint()

        paint.textSize = Game.chipTextSize
        paint.alpha = 255
        paint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        var clippedRect = rect.displayTextCenteredInRect(canvas, text, paint)

        val alpha: Bitmap = bitmap.extractAlpha()
        /* create a transparent black background to have more contrast */
        paint.color = resources.getColor(R.color.chips_background)
        canvas.drawRect(clippedRect, paint)

        /* use blurred image to create glow */
        val blurMaskFilter = BlurMaskFilter(1.5f, BlurMaskFilter.Blur.OUTER)
        paint.color = chipData.glowColor
        paint.maskFilter = blurMaskFilter
        val blurCanvas = Canvas(bitmap)
        blurCanvas.drawBitmap(alpha, 0f, 0f, paint)

        /* add the actual (non-blurred) text */
        paint.color = chipData.color
        paint.maskFilter = null
        clippedRect.displayTextCenteredInRect(canvas, text, paint)

        return bitmap
    }

    fun showUpgrades() {
        val alternatives = CopyOnWriteArrayList<ChipUpgrades>()
        when (chipData.type) {
            ChipType.EMPTY -> {
                alternatives.add(ChipUpgrades.SUB)
                alternatives.add(ChipUpgrades.SHIFT)
                //alternatives.add(ChipUpgrades.AND)
            }
            ChipType.SUB -> {
                alternatives.add(ChipUpgrades.POWERUP)
            }
            ChipType.SHIFT -> {
                alternatives.add(ChipUpgrades.POWERUP)
            }
            else -> {}
        }

        // discard the alternatives that are not allowed for this stage
        var allowed = network.theGame.currentStage?.data?.chipsAllowed ?: setOf()
        for (a in alternatives)
            if (a !in allowed)
                alternatives.remove(a)

        // calculate screen coordinates for the alternative boxes
        var posX = actualRect.centerX()
        if (network.theGame.viewport.isInRightHalfOfScreen(posX))
            posX -= (1.2 * actualRect.width()).toInt()
        else
            posX += (1.2 * actualRect.width()).toInt()
        var posY = (actualRect.centerY() - 1.5 * actualRect.height() * (alternatives.size - 1.0)).toInt()
        for (upgrade in alternatives)
        {
            val chipUpgrade = ChipUpgrade(this, upgrade,
                actualRect.centerX(), actualRect.centerY(), Color.WHITE)
            Mover(network.theGame, chipUpgrade, actualRect.centerX(), actualRect.centerY(),
                posX, posY )
            upgradePossibilities.add(chipUpgrade)
            posY += (actualRect.height() * 1.5f).toInt()
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        /* first, check if the click is inside one of the upgrade alternative boxes */
        for (upgrade in upgradePossibilities)
            upgrade.onDown(event)

        if (actualRect.contains(event.x.toInt(), event.y.toInt())
            && upgradePossibilities.isEmpty()) // gesture is inside this card
        {
            showUpgrades()
            return true
        }
        else
        {
            upgradePossibilities.clear()
            return false
        }
    }

    companion object
    {
        fun createFromData(network: Network, data: Data): Chip
                /** reconstruct an object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val gridX = data.node.gridX.toInt()
            val gridY = data.node.gridY.toInt()
            lateinit var chip: Chip
            when (data.type)
            {
                ChipType.ENTRY -> { chip = EntryPoint(network, gridX, gridY) }
                ChipType.CPU -> { chip = Cpu(network, gridX, gridY) }
                else -> { chip = Chip(network, gridX, gridY) }
            }
            chip.chipData = data
            return chip
        }
    }
}

