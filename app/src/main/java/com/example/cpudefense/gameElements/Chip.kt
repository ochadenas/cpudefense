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
    enum class ChipType { EMPTY, SUB, SHIFT, ACC, ENTRY, CPU}
    enum class ChipUpgrades { POWERUP, SUB, SHIFT, ACC }

    data class Data(
        var type: ChipType = ChipType.EMPTY,
        var power: Int = 0,
        var cooldown: Int = 0,
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

    // some chips have a 'register' where an attacker's value can be held.
    var internalRegister: Attacker? = null

    private var cooldownTimer = 0
    private var upgradePossibilities = CopyOnWriteArrayList<ChipUpgrade>()

    private var paintBitmap = Paint()
    private var paintOutline = Paint()
    private val outlineWidth = 2f
    private val paintBackground = Paint()
    private var paintLines = Paint()
    private val defaultBackgroundColor = resources.getColor(R.color.chips_background)

    init {
        actualRect = Rect()
        data.range = 2.0f
        paintOutline.color = Color.WHITE
        paintOutline.style = Paint.Style.STROKE
        paintBackground.style = Paint.Style.FILL
        paintLines.style = Paint.Style.STROKE
        paintLines.color = Color.WHITE
        paintLines.strokeWidth = 4.0f
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
                chipData.color = resources.getColor(R.color.chips_sub_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_sub_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SUB] ?: 10
                val modifier: Float = network.theGame.gameUpgrades[Upgrade.Type.INCREASE_CHIP_SUB_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (20f / modifier).toInt()
            }
            ChipType.SHIFT -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_shr_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_shr_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SHIFT] ?: 10
                val modifier: Float = network.theGame.gameUpgrades[Upgrade.Type.INCREASE_CHIP_SHIFT_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (32f / modifier).toInt()
            }
            ChipType.ACC -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_acc_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_acc_glow)
                chipData.value = Game.basePrice[ChipUpgrades.ACC] ?: 10
                val modifier: Float = network.theGame.gameUpgrades[Upgrade.Type.INCREASE_CHIP_ACC_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (16f / modifier).toInt()
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
            shootAt(attackers[0])
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
        actualRect.setCenter(viewport.gridToViewport(posOnGrid))

        /* draw background */
        paintBackground.color = defaultBackgroundColor
        paintBackground.alpha = 255
        canvas.drawRect(actualRect, paintBackground)
        if (cooldownTimer>0)
        {
            paintBackground.color = chipData.glowColor
            paintBackground.alpha = (cooldownTimer*255)/chipData.cooldown
            canvas.drawRect(actualRect, paintBackground)
        }

        /* draw outline */
        paintOutline.strokeWidth =
            if (isActivated()) 3f * outlineWidth
        else
            outlineWidth
        canvas.drawRect(actualRect, paintOutline)

        /* display a line to all vehicles in range */
        if (Game.drawLinesFromChip) {
            for (vehicle in distanceToVehicle.keys.filter { attackerInRange(it as Attacker) })
                canvas.drawLine(
                    actualRect.centerX().toFloat(), actualRect.centerY().toFloat(),
                    (viewport.gridToViewport(vehicle.posOnGrid!!)).first.toFloat(),
                    (viewport.gridToViewport(vehicle.posOnGrid!!)).second.toFloat(), paintLines
                )
        }

        if (bitmap == null)
            bitmap = createBitmapForType()
        if (bitmap != null)
            canvas.drawBitmap(bitmap!!, null, actualRect, paintBitmap)

        /* if applicable, show the different upgrade possibilities */
        for (upgrade in upgradePossibilities)
            upgrade.display(canvas)
    }

    fun attackerInRange(attacker: Attacker): Boolean
    {
        val dist: Float? = distanceTo(attacker)
        if (dist != null) {
            return dist <= data.range
        } else return false
    }

    fun attackersInRange(): List<Attacker>
    {
        val vehicles = distanceToVehicle.keys.filter { attackerInRange(it as Attacker) }.map { it as Attacker }
        return vehicles
    }

    fun shootAt(attacker: Attacker)
    {
        if (chipData.type == ChipType.EMPTY)
            return
        if (attacker.immuneTo == this)
            return
        var kill = false
        cooldownTimer = (chipData.cooldown / network.theGame.globalSpeedFactor).toInt()
        if (chipData.type == ChipType.ACC)
            processInAccumulator(attacker)
        else {
            kill = attacker.onShot(chipData.type, chipData.power)
            if (kill == true)
                attacker.remove()
        }
    }

    fun storeAttacker(attacker: Attacker?)
    {
        internalRegister = attacker
        attacker?.let {
            theNetwork?.theGame?.scoreBoard?.addCash(it.attackerData.bits)
            it.remove()
        }
    }

    fun processInAccumulator(attacker: Attacker)
    {
        if (internalRegister == null)
            storeAttacker(attacker)
        else
        {
            val number1: ULong = attacker.attackerData.number
            val number2: ULong = internalRegister?.attackerData?.number ?: 0u
            val newValue = when (chipData.power)
            {
                1 -> number1 + number2
                2 -> number1 or number2
                else -> number1 and number2
            }
            attacker.changeNumberTo(newValue)
            internalRegister = null
            attacker.immuneTo = this
        }
    }

    fun isActivated(): Boolean
            /** for display purposes: determine whether the chip is "activated",
             * depending on its type.
             */
    {
        return (chipData.type == ChipType.ACC && internalRegister != null)
    }

    private fun createBitmapForType(): Bitmap?
    {
        when (chipData.type)
        {
            ChipType.SUB -> return createBitmap("SUB %d".format(chipData.power))
            ChipType.SHIFT -> return createBitmap("SHR %d".format(chipData.power))
            ChipType.ACC -> return when (chipData.power)
            {
                1 -> createBitmap("ACC +")
                2 -> createBitmap("ACC v")
                else -> createBitmap("ACC &")
            }
            else -> return null
        }
    }

    private fun createBitmap(text: String): Bitmap?
    {
        if (actualRect.width() == 0 || actualRect.height() == 0)
            return null

        val bitmap = Bitmap.createBitmap(actualRect.width(), actualRect.height(), Bitmap.Config.ARGB_8888)
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        paint.textSize = Game.chipTextSize
        paint.alpha = 255
        paint.typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        val clippedRect = rect.displayTextCenteredInRect(canvas, text, paint)

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
                alternatives.add(ChipUpgrades.ACC)
            }
            ChipType.SUB -> {
                alternatives.add(ChipUpgrades.POWERUP)
            }
            ChipType.SHIFT -> {
                alternatives.add(ChipUpgrades.POWERUP)
            }
            ChipType.ACC -> {
                if (chipData.power < 3)
                    alternatives.add(ChipUpgrades.POWERUP)
            }
            else -> {}
        }

        // discard the alternatives that are not allowed for this stage
        val allowed = network.theGame.currentStage?.data?.chipsAllowed ?: setOf()
        for (a in alternatives)
            if (a !in allowed)
                alternatives.remove(a)

        // calculate screen coordinates for the alternative boxes
        var posX = actualRect.centerX()
        if (network.theGame.viewport.isInRightHalfOfViewport(posX))
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
            if (upgrade.onDown(event)) {
                upgradePossibilities.clear()
                return true
            }

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

