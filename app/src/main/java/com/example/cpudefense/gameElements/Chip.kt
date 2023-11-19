package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Node
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setCenter
import java.lang.Math.abs
import java.util.concurrent.CopyOnWriteArrayList

open class Chip(open val network: Network, gridX: Int, gridY: Int): Node(network, gridX.toFloat(), gridY.toFloat())
{
    enum class ChipType { EMPTY, SUB, SHR, MEM, ACC, SHL, ADD, CLK, ENTRY, CPU}
    enum class ChipUpgrades { POWERUP, REDUCE, SELL, SUB, SHR, MEM, ACC, CLK }

    data class Data(
        var type: ChipType = ChipType.EMPTY,
        var power: Int = 0,
        /** time that this chip needs before it can shoot again */
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
    private var internalRegister: Attacker? = null

    private var cooldownTimer = 0.0f
    var upgradePossibilities = CopyOnWriteArrayList<ChipUpgrade>()

    private var paintBitmap = Paint()
    private var paintOutline = Paint()
    private var outlineWidth = 2f
    private val paintBackground = Paint()
    private var paintLines = Paint()
    private val defaultBackgroundColor = resources.getColor(R.color.chips_background)

    init {
        data.range = 2.0f
        paintOutline.color = Color.WHITE
        paintOutline.style = Paint.Style.STROKE
        paintBackground.style = Paint.Style.FILL
        paintLines.style = Paint.Style.STROKE
        paintLines.color = Color.WHITE
        paintLines.strokeWidth = 4.0f
    }

    fun resetToEmptyChip()
    {
        with (chipData)
        {
            type = ChipType.EMPTY
            power = 0
            value = 0
            color = Color.WHITE
            glowColor = Color.WHITE
        }
        cooldownTimer = 0.0f
        internalRegister = null
        bitmap = null
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
                chipData.value = Game.basePrice[ChipUpgrades.SUB] ?: 99
                var modifier: Float = network.theGame.heroes[Hero.Type.INCREASE_CHIP_SUB_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (20f / modifier).toInt()
                modifier = network.theGame.heroes[Hero.Type.INCREASE_CHIP_SUB_RANGE]?.getStrength() ?: 1f
                data.range = 2f * modifier
            }
            ChipType.SHR -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_shr_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_shr_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SHR] ?: 99
                var modifier: Float = network.theGame.heroes[Hero.Type.INCREASE_CHIP_SHR_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (32f / modifier).toInt()
                modifier = network.theGame.heroes[Hero.Type.INCREASE_CHIP_SHR_RANGE]?.getStrength() ?: 1f
                data.range = 2f * modifier
            }
            ChipType.MEM -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_mem_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_mem_glow)
                chipData.value = Game.basePrice[ChipUpgrades.MEM] ?: 99
                var modifier: Float = network.theGame.heroes[Hero.Type.INCREASE_CHIP_MEM_SPEED]?.getStrength() ?: 1f
                chipData.cooldown = (128f / modifier).toInt()
                modifier = network.theGame.heroes[Hero.Type.INCREASE_CHIP_SHR_RANGE]?.getStrength() ?: 1f
                data.range = 2f * modifier
            }
            ChipType.ACC -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_acc_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_acc_glow)
                chipData.value = Game.basePrice[ChipUpgrades.ACC] ?: 99
                val modifier = 1f
                chipData.cooldown = (16f / modifier).toInt()
                data.range = 2f
            }
            ChipType.CLK -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_clk_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_clk_glow)
                chipData.value = Game.basePrice[ChipUpgrades.CLK] ?: 99
                chipData.cooldown = 52f.toInt()
                data.range = 0f
            }
            ChipType.SHL -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_shl_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_shl_glow)
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
                val modifier = 1.2f
                chipData.cooldown = (32f / modifier).toInt()
                data.range = 2f
            }
            ChipType.ADD -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_add_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_add_glow)
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
                val modifier = 1.2f
                chipData.cooldown = (20f / modifier).toInt()
                data.range = 2f
            }
            else -> {}
        }
    }

    fun addPower(amount: Int)
    {
        chipData.power += amount
        bitmap = null
    }

    fun getCooldownTime(): Float
            /** @return the number of ticks that the chip will need to cooldown.
             * For CLK chips, this depends on the level.
             */
    {
        if (this.chipData.type == ChipType.CLK)
        {
            val modifier = (1f + chipData.power)
            return chipData.cooldown.toFloat() / modifier
        }
        else
            return this.chipData.cooldown.toFloat()
    }

    fun startCooldown()
    {
        cooldownTimer = getCooldownTime()
    }

    override fun update() {
        super.update()
        if (chipData.type == ChipType.EMPTY)
            return  //  no need to calculate for empty slots
        if (cooldownTimer>0) {
            cooldownTimer -= network.theGame.globalSpeedFactor()
            return
        }
        if (chipData.type == ChipType.CLK)
        /* come here when the clock 'ticks', i.e. CLK cooldown has passed */ {
            updateClk()
            return
        }
        /* check if there are attackers that we can shoot at */
        distanceToVehicle.forEach { (vehicle, distance) ->
            val dist: Float? = distanceTo(vehicle)
            if (dist != null && dist <= data.range) {
                shootAt(vehicle as Attacker)
                return
            }
        }
    }

    fun updateClk()
    /** method that gets executed whenever the clock 'ticks' */
    {
        val chipsAffected = listOf<ChipType>( ChipType.SUB,  ChipType.SHR, ChipType.MEM )
        /*
        // make affected chip types dependent on CLK chip level
        val chipsAffected = when (chipData.power)
        {
            1 ->  listOf<ChipType>( ChipType.SUB )
            2 ->  listOf<ChipType>( ChipType.SUB, ChipType.SHR )
            3 ->  listOf<ChipType>( ChipType.SUB,  ChipType.SHR, ChipType.MEM )
            else ->  listOf<ChipType>( ChipType.SUB,  ChipType.SHR, ChipType.MEM )
        }
         */
        for (node in theNetwork.nodes.values)
        {
            val chip = node as Chip
            if (chip.chipData.type in chipsAffected) {
                // avoid resetting when clock tick comes _too_ soon after the regular reset
                if (chip.cooldownTimer < chip.getCooldownTime() * 0.8f)
                    chip.cooldownTimer = 0f
            }
        }
        startCooldown()
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (chipData.type == ChipType.ENTRY)
            return super.display(canvas, viewport)  // TODO. temporary, just as long as ENTRY does not have its own display method

        /* calculate size */
        /* this is put here because the viewport / zoom factor may change.
        However, it may be possible to remove this from display()
         */
        val sizeOnScreen = theNetwork.distanceBetweenGridPoints()
        sizeOnScreen?.let {
            widthOnScreen = it.first * Game.chipSize.x.toInt()
            heightOnScreen = it.second * Game.chipSize.y.toInt()
            actualRect = Rect(0, 0, widthOnScreen, heightOnScreen)
        }
        outlineWidth = 2f * theNetwork.theGame.resources.displayMetrics.scaledDensity
        actualRect?.setCenter(viewport.gridToViewport(posOnGrid))
        actualRect?.let { displayRect(canvas, it) }
        if (network.theGame.gameActivity.settings.configShowAttsInRange && chipData.type != ChipType.EMPTY)
            actualRect?.let { displayLineToAttacker(canvas, attackersInRange(), it) }
    }

    private fun displayLineToAttacker(canvas: Canvas, attackersInRange: List<Attacker>, chipRect: Rect)
    {
        paintLines.color = chipData.color
        attackersInRange.forEach { att ->
                canvas.drawLine(chipRect.exactCenterX(), chipRect.exactCenterY(), att.actualRect.exactCenterX(), att.actualRect.exactCenterY(), paintLines)
        }
    }

    private fun displayRect(canvas: Canvas, rect: Rect)
    {
        /* draw background */
        paintBackground.color = defaultBackgroundColor
        paintBackground.alpha = 255
        canvas.drawRect(rect, paintBackground)
        if (cooldownTimer>0)
        {
            paintBackground.color = chipData.glowColor
            paintBackground.alpha = (cooldownTimer*255f/getCooldownTime()).toInt()
            canvas.drawRect(rect, paintBackground)
        }

        /* draw outline */
        paintOutline.strokeWidth =
            if (isActivated()) 3f * outlineWidth
            else
                outlineWidth
        canvas.drawRect(rect, paintOutline)

        /* draw foreground */
        if (bitmap == null)
            bitmap = createBitmapForType()
        bitmap?.let { canvas.drawBitmap(it, null, rect, paintBitmap) }
    }

    fun displayUpgrades(canvas: Canvas)
    /** if applicable, show the different upgrade possibilities */
    {
        for (upgrade in upgradePossibilities)
            upgrade.display(canvas)
    }

    private fun attackerInRange(attacker: Attacker): Boolean
    {
        val dist: Float? = distanceTo(attacker)
        if (dist != null) {
            return dist <= abs(data.range)
        } else return false
    }

    fun attackersInRange(): List<Attacker>
    {
        try {
            val vehicles = distanceToVehicle.keys.filter { attackerInRange(it as Attacker) }
                .map { it as Attacker }
            return vehicles
        }
        catch (ex: ConcurrentModificationException)
        { return listOf() }  // may happen when there are really many chips and attackers
    }

    fun shootAt(attacker: Attacker)
    {
        if (chipData.type == ChipType.EMPTY)
            return
        if (attacker.immuneTo == this)
            return
        startCooldown()
        if (chipData.type == ChipType.ACC)
        {
            if (attacker.attackerData.isCoin)
                return // ACC does not affect coins
            else
                processInAccumulator(attacker)
        }
        else if (attacker.onShot(chipData.type, chipData.power))
            attacker.remove()
    }

    fun storeAttacker(attacker: Attacker?)
    {
        internalRegister = attacker
        attacker?.let {
            val extraCashGained = theNetwork.theGame.heroes[Hero.Type.GAIN_CASH_ON_KILL]?.getStrength()?.toInt() ?: 0 // possible bonus
            theNetwork.theGame.scoreBoard.addCash(it.attackerData.bits + extraCashGained)
            it.immuneTo = this
            theNetwork.theGame.gameActivity.theGameView.theEffects?.fade(it)
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
            ChipType.SHR -> return createBitmap("SHR %d".format(chipData.power))
            ChipType.MEM -> return createBitmap("MEM")
            ChipType.ACC -> return when (chipData.power)
            {
                1 -> createBitmap("ACC +")
                2 -> createBitmap("ACC v")
                else -> createBitmap("ACC &")
            }
            ChipType.SHL -> return createBitmap("SHL %d".format(chipData.power))
            ChipType.ADD -> return createBitmap("ADD %d".format(chipData.power))
            ChipType.CLK -> return createBitmap("CLK %d".format(chipData.power))
            else -> return null
        }
    }

    private fun createBitmap(text: String): Bitmap?
    {
        val bitmap: Bitmap? = actualRect?.let {
            val bitmap = Bitmap.createBitmap(it.width(), it.height(), Bitmap.Config.ARGB_8888)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            paint.textSize = (Game.chipTextSize * resources.displayMetrics.scaledDensity) *
                    if (theNetwork.theGame.gameActivity.settings.configUseLargeButtons) 1.2f else 1.0f
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
            bitmap
        }
        return bitmap
    }

    fun showUpgrades() {
        val alternatives = CopyOnWriteArrayList<ChipUpgrades>()
        when (chipData.type) {
            ChipType.EMPTY -> {
                alternatives.add(ChipUpgrades.SUB)
                alternatives.add(ChipUpgrades.SHR)
                alternatives.add(ChipUpgrades.ACC)
                alternatives.add(ChipUpgrades.MEM)
                if (theNetwork.theGame.currentStage?.chipCount(ChipType.CLK) == 0)
                    alternatives.add(ChipUpgrades.CLK) // only one allowed
            }
            ChipType.SUB -> {
                alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.SHR -> {
                alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.ACC -> {
                if (chipData.power < 3)
                    alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.MEM -> {
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.CLK -> {
                alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.SHL -> {
                alternatives.add(ChipUpgrades.REDUCE)
            }
            ChipType.ADD -> {
                alternatives.add(ChipUpgrades.REDUCE)
            }
                else -> {}
        }

        // discard the alternatives that are not allowed for this stage,
        // but only for series 1.
        // In series 2, all upgrades are always possible
        if (network.theGame.currentStage?.getSeries() == 1) {
            val allowed = network.theGame.currentStage?.data?.chipsAllowed ?: setOf()
            for (a in alternatives)
                if (a !in allowed)
                    alternatives.remove(a)
        }
        // discard the possibility for upgrade if the chip is already very high powered
        if (chipData.power >= 12)
        {
            alternatives.remove(ChipUpgrades.POWERUP)
        }

        // calculate screen coordinates for the alternative boxes
        actualRect?.let { rect ->
            val posX = rect.centerX()
            val posY = rect.centerY()
            val positions = listOf(
                Pair(1.0f, -0.5f),
                Pair(1.0f, +0.5f),
                Pair(2.0f, -0.5f),
                Pair(2.0f, +0.5f),
                Pair(3.0f, -0.5f),
                Pair(3.0f, +0.5f)
            )
            val factorY = 1.5 * rect.height()
            val factorX: Float
            if (network.theGame.viewport.isInRightHalfOfViewport(posX))
                factorX = -1.2f * rect.width()
            else
                factorX = +1.2f * rect.width()
            for ((i, upgrade) in alternatives.withIndex()) {
                val chipUpgrade = ChipUpgrade(
                    this, upgrade,
                    rect.centerX(), rect.centerY(), Color.WHITE
                )
                val pos: Pair<Float, Float> = positions[i]
                Mover(
                    network.theGame, chipUpgrade, rect.centerX(), rect.centerY(),
                    posX + (pos.first * factorX).toInt(), posY + (pos.second * factorY).toInt()
                )
                upgradePossibilities.add(chipUpgrade)
            }
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        /* first, check if the click is inside one of the upgrade alternative boxes */
        /*
        for (upgrade in upgradePossibilities)
            if (upgrade.onDown(event)) {
                upgradePossibilities.clear()
                return true
            }

         */

        if (actualRect?.contains(event.x.toInt(), event.y.toInt()) == true
            && upgradePossibilities.isEmpty()) // gesture is inside this chip
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
