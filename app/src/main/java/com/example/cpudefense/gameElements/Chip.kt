package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Node
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.drawOutline
import com.example.cpudefense.utils.inflate
import com.example.cpudefense.utils.setCenter
import java.lang.Math.abs
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

open class Chip(val network: Network, gridX: Int, gridY: Int): Node(network, gridX.toFloat(), gridY.toFloat())
{
    enum class ChipType { EMPTY, SUB, SHR, MEM, ACC, SHL, ADD, NOP, CLK, ENTRY, CPU}
    enum class ChipUpgrades { POWERUP, REDUCE, SELL, SUB, SHR, MEM, ACC, CLK }

    data class Data(
        var type: ChipType = ChipType.EMPTY,
        var power: Int = 0,
        /** time that this chip needs before it can shoot again */
        var cooldown: Int = 0,
        /** current state of the timer */
        var cooldownTimer: Float = 0.0f,
        var value: Int = 0,
        var node: Node.Data,
        var color: Int = Color.WHITE,
        var glowColor: Int = Color.WHITE,
        var sold: Boolean = false  // this is an indicator that the chip has been sold, but is not removed yet
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

    var upgradePossibilities = CopyOnWriteArrayList<ChipUpgrade>()

    private var paintBitmap = Paint()
    private var paintOutline = Paint()
    private var paintUpgradesBackground = Paint()
    private var outlineWidth = 2f
    private val paintBackground = Paint()
    private var paintLines = Paint()
    private val defaultBackgroundColor = resources.getColor(R.color.chips_background)

    init {
        data.range = 2.0f
        paintOutline.color = Color.WHITE
        paintOutline.style = Paint.Style.STROKE
        paintBackground.style = Paint.Style.FILL
        paintBackground.alpha = 255
        paintLines.style = Paint.Style.STROKE
        paintLines.color = Color.WHITE
        paintLines.strokeWidth = 4.0f
        paintUpgradesBackground.color = Color.BLACK
        paintUpgradesBackground.strokeWidth = 16.0f
        paintUpgradesBackground.style = Paint.Style.FILL
        paintUpgradesBackground.alpha = 240
    }

    fun sellChip()
            /** marks this chip as sold, but does not remove it directly.
             * Final removal is done after the cooldown expires.
             */
    {
        with (chipData)
        {
            if (cooldownTimer > 0.0f) {
                color = resources.getColor(R.color.chips_soldstate_foreground)
                glowColor = resources.getColor(R.color.chips_soldstate_glow)
                sold = true
                bitmap = null // force re-draw of the chip, using new colours
            } else
                resetToEmptyChip()
        }
    }

    fun resetToEmptyChip()
    /** called when a sold chip is definitely removed */
    {
        with (chipData)
        {
            type = ChipType.EMPTY
            power = 0
            value = 0
            color = Color.WHITE
            glowColor = Color.WHITE
            sold = false
            cooldownTimer = 0.0f
        }
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
                chipData.cooldown = (72f / modifier).toInt() // was 128f
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
            ChipType.NOP -> {
                chipData.power = 1
                bitmap = null
                chipData.color = resources.getColor(R.color.chips_noop_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_noop_glow)
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
            }
            else -> {}
        }
        if (chipData.sold)
        {
            chipData.color = resources.getColor(R.color.chips_soldstate_foreground)
            chipData.glowColor = resources.getColor(R.color.chips_soldstate_glow)
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
        chipData.cooldownTimer = getCooldownTime()
    }

    override fun update() {
        super.update()
        if (chipData.type == ChipType.EMPTY)
            return  //  no need to calculate for empty slots
        if (chipData.cooldownTimer>0) {
            chipData.cooldownTimer -= network.theGame.globalSpeedFactor()
            return
        }
        if (chipData.sold) {
            resetToEmptyChip()
            return
        }
        if (chipData.type == ChipType.CLK)
        /* come here when the clock 'ticks', i.e. CLK cooldown has passed */ {
            updateClk()
            return
        }
        /* check if there are attackers that we can shoot at */
        try {
            val vehiclesInRange = distanceToVehicle.toList().filter {distanceTo(it.first)?.let { it <= data.range } ?: false }
            if (!vehiclesInRange.isEmpty()) {
                val possibleTargets = vehiclesInRange.sortedBy { (it.first as Attacker).attackerData.number }
                val target = when (this.chipData.type)
                {
                    ChipType.SUB -> possibleTargets.first().first
                    else -> possibleTargets.last().first
                }
                shootAt(target as Attacker)
            }
        }
        catch (exception: ConcurrentModificationException)
        {
            // just ignore this
        }
    }

    fun updateClk()
    /** method that gets executed whenever the clock 'ticks' */
    {
        val chipsAffected = listOf<ChipType>( ChipType.SUB,  ChipType.SHR, ChipType.MEM )
        for (node in theNetwork.nodes.values)
        {
            val chip = node as Chip
            if (chip.chipData.type in chipsAffected) {
                // avoid resetting when clock tick comes _too_ soon after the regular reset
                val minDelay = kotlin.math.min(chip.getCooldownTime() * 0.2f, this.getCooldownTime())
                if (chip.chipData.cooldownTimer <= chip.getCooldownTime()-minDelay) {
                    var generatedHeat = chip.chipData.cooldownTimer
                    val factor = 100f - (network.theGame.heroes[Hero.Type.REDUCE_HEAT]?.getStrength() ?: 0f)
                    generatedHeat *= (factor * Game.heatAdjustmentFactor / 100f)
                    theNetwork.theGame.state.heat += generatedHeat
                    chip.chipData.cooldownTimer = 0f
                }
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
        if (chipData.cooldownTimer>0)
        {
            paintBackground.color = chipData.glowColor
            paintBackground.alpha = (chipData.cooldownTimer*255f/getCooldownTime()).toInt()
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
        if (upgradePossibilities.size == 0)
            return
        var upgradesArea = Rect(0,0,0,0)  // start with empty rect
        /* create a rectangle that contains all update boxes including their labels */
        for (upgrade in upgradePossibilities) {
            upgradesArea.union(upgrade.actualRect)
            upgradesArea.union(upgrade.labelRect)
        }
        upgradesArea.inflate(16)
        canvas.drawRect(upgradesArea, paintUpgradesBackground)
        /* display the upgrade boxes */
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
        if (chipData.type in listOf<ChipType>(ChipType.ACC, ChipType.MEM, ChipType.CLK)
            && attacker.attackerData.isCoin)
            return  // coins are unaffected by certain chip types
        when (chipData.type)
        {
            ChipType.ACC -> { processInAccumulator(attacker) }
            ChipType.MEM -> {
                if (internalRegister == null)
                    storeAttacker(attacker)
                return
            }
            else -> {
                if (attacker.onShot(chipData.type, chipData.power))
                    attacker.remove()
            }
        }
        startCooldown()
    }

    fun storeAttacker(attacker: Attacker)
    {
        internalRegister = attacker
        val extraCashGained =
            theNetwork.theGame.heroes[Hero.Type.GAIN_CASH_ON_KILL]?.getStrength()?.toInt()
                ?: 0 // possible bonus
        theNetwork.theGame.scoreBoard.addCash(attacker.attackerData.bits + extraCashGained)
        attacker.immuneTo = this
        theNetwork.theGame.gameActivity.theGameView.theEffects?.fade(attacker)
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
            var change_in_speed = attacker.data.speed * (Random.nextFloat() - 0.5f) * 0.3f
            attacker.data.speed += change_in_speed
            attacker.setCurrentSpeed()
            internalRegister = null
            attacker.immuneTo = this
        }
    }

    fun isActivated(): Boolean
            /** for display purposes: determine whether the chip is "activated",
             * depending on its type.
             */
    {
        return (chipData.type in listOf(ChipType.ACC, ChipType.MEM) && internalRegister != null)
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
            ChipType.NOP -> return if (chipData.power == 1)  createBitmap("NOP")
                else createBitmap("NOP %d".format(chipData.power))
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
        if (chipData.sold)
            return
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
            ChipType.NOP -> {
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
            val factorY = 1.6 * rect.height()
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
        if (actualRect?.contains(event.x.toInt(), event.y.toInt()) == false)
        // gesture is inside this chip
        {
            upgradePossibilities.clear()
            return false
        }
        if (chipData.type == ChipType.MEM)
            // activated MEM chips are cleared through tapping
        {
            if (isActivated())
            {
                internalRegister = null
                startCooldown()
                return true
            }
        }

        if (upgradePossibilities.isEmpty())
            showUpgrades()
        return true
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
