@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import com.example.cpudefense.*
import com.example.cpudefense.effects.Mover
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Node
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.inflate
import com.example.cpudefense.utils.setBottomLeft
import com.example.cpudefense.utils.setCenter
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.exp
import kotlin.random.Random

open class Chip(val network: Network, gridX: Int, gridY: Int): Node(network, gridX.toFloat(), gridY.toFloat())
{
    enum class ChipType { EMPTY, SUB, SHR, MEM, ACC, RES, SHL, ADD, NOP, SPLT, DUP, CLK, ENTRY, CPU}
    enum class ChipUpgrades { POWERUP, REDUCE, SELL, SUB, SHR, MEM, ACC, CLK, RES }

    data class Data(
        /** the principal type of the chip */
        var type: ChipType = ChipType.EMPTY,
        /** level, or strength, of the chip */
        var upgradeLevel: Int = 0,
        /** time that this chip needs before it can shoot again */
        var cooldown: Int = 0,
        /** current state of the timer */
        var cooldownTimer: Float = 0.0f,
        /** the price paid for the chip and its upgrades */
        var value: Int = 0,
        var node: Node.Data,
        var color: Int = Color.WHITE,
        var glowColor: Int = Color.WHITE,
        /** indicator that the chip has been sold, but is not removed yet */
        var sold: Boolean = false
    )

    var chipData = Data(
        node = super.data
    )

    open var bitmap: Bitmap? = null
    private var widthOnScreen: Int = 0
    private var heightOnScreen = 0

    val resources = network.theGame.resources
    private val resistorColour = arrayOf(
        resources.getColor(R.color.resistor_0),
        resources.getColor(R.color.resistor_1),
        resources.getColor(R.color.resistor_2),
        resources.getColor(R.color.resistor_3),
        resources.getColor(R.color.resistor_4),
        resources.getColor(R.color.resistor_5),
        resources.getColor(R.color.resistor_6),
        resources.getColor(R.color.resistor_7),
        resources.getColor(R.color.resistor_8),
        resources.getColor(R.color.resistor_9),
    )

    private var chipsThatDoNotAffectCoins = listOf(ChipType.ACC, ChipType.MEM, ChipType.CLK, ChipType.SPLT, ChipType.DUP)
    private var chipsAffectedByCLK = listOf( ChipType.SUB, ChipType.SHR, ChipType.MEM )

    var internalRegister = Register()

    var upgradePossibilities = CopyOnWriteArrayList<ChipUpgrade>()

    private var paintBitmap = Paint()
    private var paintOutline = Paint()
    private var paintIndicator = Paint()
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
        paintIndicator.color = paintOutline.color
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
            internalRegister.releaseAll() // if an attacker is held inside, release it. This is for MEM and ACC
            if (isInCooldown()) {
                color = resources.getColor(R.color.chips_soldstate_foreground)
                glowColor = resources.getColor(R.color.chips_soldstate_glow)
                sold = true
                bitmap = null // force re-draw of the chip, using new colours
            } else
                resetToEmptyChip()
        }
    }

    private fun resetToEmptyChip()
    /** called when a sold chip is definitely removed */
    {
        with (chipData)
        {
            type = ChipType.EMPTY
            upgradeLevel = 0
            value = 0
            color = Color.WHITE
            glowColor = Color.WHITE
            sold = false
            cooldownTimer = 0.0f
        }
        bitmap = null
    }
    fun setIdent(ident: Int)
    {
        data.ident = ident
    }

    fun setType(chipType: ChipType)
    {
        chipData.type = chipType
        chipData.upgradeLevel = 1
        bitmap = null
        val game = network.theGame
        when (chipType)
        {
            ChipType.SUB -> {

                chipData.color = resources.getColor(R.color.chips_sub_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_sub_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SUB] ?: 99
                chipData.cooldown = (20f / game.heroModifier(Hero.Type.INCREASE_CHIP_SUB_SPEED)).toInt()
                data.range = 2f * game.heroModifier(Hero.Type.INCREASE_CHIP_SUB_RANGE)
            }
            ChipType.SHR -> {
                chipData.color = resources.getColor(R.color.chips_shr_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_shr_glow)
                chipData.value = Game.basePrice[ChipUpgrades.SHR] ?: 99
                chipData.cooldown = (32f / game.heroModifier(Hero.Type.INCREASE_CHIP_SHR_SPEED)).toInt()
                data.range = 2f * game.heroModifier(Hero.Type.INCREASE_CHIP_SHR_RANGE)
            }
            ChipType.MEM -> {
                chipData.color = resources.getColor(R.color.chips_mem_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_mem_glow)
                chipData.value = Game.basePrice[ChipUpgrades.MEM] ?: 99
                chipData.cooldown = (72f / game.heroModifier(Hero.Type.INCREASE_CHIP_MEM_SPEED)).toInt()
                data.range = 2f * game.heroModifier(Hero.Type.INCREASE_CHIP_MEM_RANGE)
            }
            ChipType.ACC -> {
                chipData.color = resources.getColor(R.color.chips_acc_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_acc_glow)
                chipData.value = Game.basePrice[ChipUpgrades.ACC] ?: 99
                chipData.cooldown = 16 // fixed value
                data.range = 2f
            }
            ChipType.RES -> {
                chipData.color = resources.getColor(R.color.chips_resistor_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_resistor_glow)
                chipData.value = Game.basePrice[ChipUpgrades.RES] ?: 99
                chipData.cooldown = 4 // fixed value
                data.range = 2f
            }
            ChipType.CLK -> {
                chipData.color = resources.getColor(R.color.chips_clk_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_clk_glow)
                chipData.value = Game.basePrice[ChipUpgrades.CLK] ?: 99
                chipData.cooldown = 52f.toInt()
                data.range = 0f
            }
            ChipType.SHL -> {
                chipData.color = resources.getColor(R.color.chips_shl_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_shl_glow)
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
                val modifier = 1.2f
                chipData.cooldown = (32f / modifier).toInt()
                data.range = 2f
            }
            ChipType.ADD -> {
                chipData.color = resources.getColor(R.color.chips_add_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_add_glow)
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
                val modifier = 1.2f
                chipData.cooldown = (20f / modifier).toInt()
                data.range = 2f
            }
            ChipType.NOP -> {
                chipData.color = resources.getColor(R.color.chips_noop_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_noop_glow)
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
            }
            ChipType.SPLT -> {
                chipData.color = resources.getColor(R.color.chips_split_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_split_glow)
                val modifier = 1.2f
                chipData.cooldown = (24f / modifier).toInt()
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
            }
            ChipType.DUP -> {
                chipData.color = resources.getColor(R.color.chips_split_foreground)
                chipData.glowColor = resources.getColor(R.color.chips_split_glow)
                val modifier = 1.2f
                chipData.cooldown = (32f / modifier).toInt()
                chipData.value = Game.basePrice[ChipUpgrades.REDUCE] ?: 99
            }
            ChipType.ENTRY -> { chipData.upgradeLevel = 0}
            ChipType.CPU -> { chipData.upgradeLevel = 0}
            ChipType.EMPTY -> { chipData.upgradeLevel = 0}
        }
        if (chipData.sold)
        {
            chipData.color = resources.getColor(R.color.chips_soldstate_foreground)
            chipData.glowColor = resources.getColor(R.color.chips_soldstate_glow)
        }
    }

    fun addPower(amount: Int)
    {
        chipData.upgradeLevel += amount
        bitmap = null
    }

    fun obstacleDifficulty(): Double
    {
        return (obstacleStrength[chipData.type] ?: 0.0) * chipData.upgradeLevel
    }

    private fun resistorValue(): Int
    {
        if (chipData.type != ChipType.RES)
            return 0
        val resistance = chipData.upgradeLevel * Game.resistorBaseStrength * theNetwork.theGame.heroModifier(Hero.Type.INCREASE_CHIP_RES_STRENGTH)
        return resistance.toInt()
    }

    private fun getCooldownTime(): Float
            /** @return the number of ticks that the chip will need to cooldown.
             * For CLK chips, this depends on the level.
             */
    {
        if (this.chipData.type == ChipType.CLK)
        {
            val modifier = (1f + chipData.upgradeLevel)
            return chipData.cooldown.toFloat() / modifier
        }
        else
            return this.chipData.cooldown.toFloat()
    }

    private inline fun isInCooldown(): Boolean
    /** @return true if the chip is in its cooldown phase */
    {
        return chipData.cooldownTimer > 0.0f
    }

    private fun startCooldown()
    {
        chipData.cooldownTimer = getCooldownTime()
    }

    override fun update() {
        super.update()
        if (chipData.type == ChipType.EMPTY)
            return  //  no need to calculate for empty slots
        if (isInCooldown()) {
            chipData.cooldownTimer -= network.theGame.globalSpeedFactor()
            if (chipData.type != ChipType.MEM)  // MEM is the only type that may act during cooldown
                return
        }
        if (chipData.sold)
        {
            if (!isInCooldown())
                resetToEmptyChip()
            return   // chips already sold do not act. They only wait to be removed after cooldown
        }
        if (chipData.type == ChipType.CLK)
        /* come here when the clock 'ticks', i.e. CLK cooldown has passed */ {
            updateClk()
            return
        }
        /* check if there are attackers that we can shoot at */
        val attackers = attackersInRange()
        if (attackers.isEmpty())
            return // no need to check
        selectTarget(attackers)?.let { shootAt(it)}
    }

    private fun selectTarget(attackerList: List<Attacker>): Attacker?
    /** intelligently determine the targeted attacker, based on chip type and attacker's properties */
    {
        val possibleTargets = attackerList.filter { it.data.state == Vehicle.State.ACTIVE }
        val coins = possibleTargets.filter { it.attackerData.isCoin }
        val regularAttackers = possibleTargets.filter { !it.attackerData.isCoin }
        val sortedTargets = regularAttackers.sortedBy { it.attackerData.number }
        // sortedTargets is a list of regular attackers, smallest value first.
        // Depending on the chip type, prioritize either small values or large values or coins.
        try {
        return when (this.chipData.type)
        {
            ChipType.SUB -> {(coins + sortedTargets).first()}
            ChipType.SHR -> {(sortedTargets + coins).last()}
            ChipType.ACC -> {sortedTargets.last()}
            ChipType.MEM -> {sortedTargets.last()}
            ChipType.SPLT -> {sortedTargets.last()}
            ChipType.DUP -> {sortedTargets.last()}
            else -> {(sortedTargets + coins).last()}
        }}
        catch (ex: NoSuchElementException)
        {
            return null // no matching attackers in range
        }
    }

    private fun updateClk()
    /** method that gets executed whenever the clock 'ticks' */
    {
        for (node in theNetwork.nodes.values)
        {
            val chip = node as Chip
            if (chip.chipData.type in chipsAffectedByCLK) {
                // avoid resetting when clock tick comes _too_ soon after the regular reset
                val minDelay = kotlin.math.min(chip.getCooldownTime() * 0.2f, this.getCooldownTime())
                if (chip.chipData.cooldownTimer <= chip.getCooldownTime()-minDelay) {
                    var generatedHeat = chip.chipData.cooldownTimer
                    val factor = 100f - network.theGame.heroModifier(Hero.Type.REDUCE_HEAT)
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
            return super.display(canvas, viewport)

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
        if (isInCooldown())
        {
            paintBackground.color = chipData.glowColor
            paintBackground.alpha = (chipData.cooldownTimer*255f/getCooldownTime()).toInt()
            canvas.drawRect(rect, paintBackground)
        }

        /* special treatment for chips that store values */
        if (chipData.type in listOf(ChipType.MEM, ChipType.ACC))
            internalRegister.display(canvas, rect)

        /* draw foreground */
        if (bitmap == null)
            bitmap = createBitmapForType()
        bitmap?.let { canvas.drawBitmap(it, null, rect, paintBitmap) }

        /* draw outline */
        paintOutline.strokeWidth =
            if (chipData.type == ChipType.MEM && isActivated() && !isInCooldown()) 3f * outlineWidth
            else
                outlineWidth
        canvas.drawRect(rect, paintOutline)

    }

    fun displayUpgrades(canvas: Canvas)
    /** if applicable, show the different upgrade possibilities */
    {
        if (upgradePossibilities.size == 0)
            return
        val upgradesArea = Rect(0,0,0,0)  // start with empty rect
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

    override fun drawConnectorsOnLinks(): Boolean
    { return (chipData.type == ChipType.ENTRY) }

    @Suppress("UNCHECKED_CAST")
    fun attackersInRange(): List<Attacker>
    {
        return vehiclesInRange(data.range) as List<Attacker>
    }

    private fun shootAt(attacker: Attacker)
    {
        if (chipData.type == ChipType.EMPTY)
            return
        if (attacker.immuneTo == this || attacker.immuneToAll)
            return
        if (chipData.type in chipsThatDoNotAffectCoins
            && attacker.attackerData.isCoin)
            return  // coins are unaffected by certain chip types
        when (chipData.type)
        {
            ChipType.ACC -> { processInAccumulator(attacker) }
            ChipType.SPLT -> { splitAttacker(attacker) }
            ChipType.DUP -> { duplicateAttacker(attacker) }
            ChipType.MEM -> {
                if (slotsLeftInMEM())
                    internalRegister.store(attacker)
                return // no cooldown phase here
            }
            ChipType.RES -> {
                attacker.data.speedModifier = effectOfResistanceOnSpeed(resistorValue().toFloat())
                val additionalDuration = Game.resistorBaseDuration / attacker.data.speedModifier * theNetwork.theGame.heroModifier(Hero.Type.INCREASE_CHIP_RES_DURATION)
                attacker.data.speedModificationTimer += additionalDuration
                attacker.immuneTo = this
                attacker.makeNumber()
            }
            else -> {
                if (attacker.onShot(chipData.type, chipData.upgradeLevel))
                    attacker.remove()
            }
        }
        startCooldown()
    }

    private fun slotsLeftInMEM(): Boolean
    /** @return true if the MEM chip can still hold another number */
    {
        val slotsLeft = when (isInCooldown())
        {
            true -> internalRegister.slotsUsed()+1 < chipData.upgradeLevel
            false -> internalRegister.slotsUsed() < chipData.upgradeLevel
        }
        return slotsLeft
    }

    private fun splitAttacker(attacker: Attacker): Attacker
    {
        val newAttacker = duplicateAttacker(attacker)
        val number = attacker.attackerData.number
        when (newAttacker.attackerData.bits)
        {
            in 0..4 -> {
                attacker.attackerData.number = number and 0x03u
            }
            in 5 .. 8 -> {
                attacker.attackerData.number = number and 0x0Fu
            }
            in 9 .. 16 -> {
                attacker.attackerData.number = number and 0x00FFu
            }
            else ->
            {
                attacker.attackerData.number = number and 0xFFFFu
            }
        }
        newAttacker.attackerData.number -= attacker.attackerData.number
        attacker.makeNumber()
        newAttacker.makeNumber()
        return newAttacker
    }

    private fun duplicateAttacker(attacker: Attacker): Attacker
    {
        var changeInSpeed = attacker.data.speed * (Random.nextFloat() * 0.1f)
        attacker.data.speed += changeInSpeed
        val newAttacker = attacker.copy()
        changeInSpeed = newAttacker.data.speed * (Random.nextFloat() * 0.2f + 0.1f)
        newAttacker.data.speed -= changeInSpeed
        network.addVehicle(newAttacker)
        newAttacker.setOntoLink(attacker.onLink, newAttacker.startNode)
        attacker.immuneTo = this
        newAttacker.immuneTo = this
        return newAttacker
    }

    private fun processInAccumulator(attacker: Attacker)
    {
        val previousAttacker = internalRegister.retrieve()
        if (previousAttacker == null)
            internalRegister.store(attacker)
        else
        {
            val number1: ULong = attacker.attackerData.number
            val number2: ULong = previousAttacker.attackerData.number
            val newValue = when (chipData.upgradeLevel)
            {
                1 -> number1 + number2
                2 -> number1 or number2
                else -> number1 and number2
            }
            attacker.changeNumberTo(newValue)
            attacker.jitterSpeed()
            internalRegister.clear()
            attacker.immuneTo = this
        }
    }

    private fun effectOfResistanceOnSpeed(ohm: Float): Float
    {
        return exp(- ohm / 74.0f)
    }

    private fun isActivated(): Boolean
            /** for display purposes: determine whether the chip is "activated",
             * depending on its type.
             */
    {
        return (internalRegister.slotsUsed()>0)
    }

    private fun createBitmapForType(): Bitmap?
    {
        return when (chipData.type)
        {
            ChipType.SUB -> createBitmap("SUB%2d".format(chipData.upgradeLevel))
            ChipType.SHR -> createBitmap("SHR%2d".format(chipData.upgradeLevel))
            ChipType.MEM -> { if (chipData.upgradeLevel == 1) createBitmap("MEM")
                else createBitmap("MEM%2d".format(chipData.upgradeLevel)) }
            ChipType.ACC -> when (chipData.upgradeLevel)
            {
                1 -> createBitmap("ACC +")
                2 -> createBitmap("ACC v")
                else -> createBitmap("ACC &")
            }
            ChipType.SHL -> createBitmap("SHL%2d".format(chipData.upgradeLevel))
            ChipType.ADD -> createBitmap("ADD%2d".format(chipData.upgradeLevel))
            ChipType.CLK -> createBitmap("CLK%2d".format(chipData.upgradeLevel))
            ChipType.RES -> createBitmapForResistor()
            ChipType.SPLT -> createBitmap("SPLT")
            ChipType.DUP -> createBitmap("DUP")
            ChipType.NOP -> if (chipData.upgradeLevel == 1)  createBitmap("NOP")
                else createBitmap("NOP%2d".format(chipData.upgradeLevel))
            ChipType.ENTRY -> null
            ChipType.CPU -> null
            ChipType.EMPTY -> null
        }
    }

    private fun createBitmapForResistor(): Bitmap?
    /** special colour-coded symbols for chips of type resistor */
    {
        val bitmap: Bitmap? = actualRect?.let {
            val bitmap = Bitmap.createBitmap(it.width(), it.height(), Bitmap.Config.ARGB_8888)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            val widthOfRings = rect.width() / 10
            val gapBetweenRings = (widthOfRings * 0.8f).toInt()
            val leftMarginOfRings = rect.width() / 5
            val heightOfRings = rect.height()

            var ohm = resistorValue()
            var lastDigit = ohm % 10
            var multiplier = 0
            for (digit in 1 .. 9)
            {
                if (ohm>=10)
                {
                    lastDigit = ohm % 10
                    ohm /= 10
                    multiplier = digit
                }
                else
                    break
            }
            val firstDigit: Int = ohm
            try {
                // 1st ring
                var left = leftMarginOfRings
                var ringRect = Rect(left, 0, left + widthOfRings, heightOfRings)
                paint.color = resistorColour[firstDigit]
                canvas.drawRect(ringRect, paint)

                // 2nd ring
                left = leftMarginOfRings + widthOfRings + gapBetweenRings
                ringRect = Rect(left, 0, left + widthOfRings, heightOfRings)
                paint.color = resistorColour[lastDigit]
                canvas.drawRect(ringRect, paint)

                // 3rd ring
                left = leftMarginOfRings + 2 * (widthOfRings + gapBetweenRings)
                ringRect = Rect(left, 0, left + widthOfRings, heightOfRings)
                paint.color = resistorColour[multiplier]
                canvas.drawRect(ringRect, paint)
            }
            catch (exception: NoSuchElementException) {}

            bitmap
        }
        return bitmap
    }

    private fun createBitmap(text: String): Bitmap?
    {
        val bitmap: Bitmap? = actualRect?.let {
            val bitmap = Bitmap.createBitmap(it.width(), it.height(), Bitmap.Config.ARGB_8888)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val canvas = Canvas(bitmap)
            val paint = Paint()

            paint.textSize = (Game.chipTextSize * resources.displayMetrics.scaledDensity) *
                    if (theNetwork.theGame.gameActivity.settings.configUseLargeButtons) 1.0f else 0.95f // multiple sizes possible
            paint.alpha = 255
            paint.typeface = ResourcesCompat.getFont(network.theGame.gameActivity, R.font.ubuntu_mono_bold)
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

    private fun showUpgrades() {
        if (chipData.sold)
            return
        val alternatives = CopyOnWriteArrayList<ChipUpgrades>()
        when (chipData.type) {
            ChipType.EMPTY -> {
                alternatives.add(ChipUpgrades.SUB)
                alternatives.add(ChipUpgrades.SHR)
                alternatives.add(ChipUpgrades.ACC)
                alternatives.add(ChipUpgrades.MEM)
                alternatives.add(ChipUpgrades.RES)
                if (theNetwork.theGame.currentlyActiveStage?.chipCount(ChipType.CLK) == 0)
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
                if (chipData.upgradeLevel < 3)
                    alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.MEM -> {
                if (chipData.upgradeLevel < theNetwork.theGame.actualMaxInternalChipStorage())
                    alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.CLK -> {
                alternatives.add(ChipUpgrades.POWERUP)
                alternatives.add(ChipUpgrades.SELL)
            }
            ChipType.RES -> {
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
            ChipType.SPLT -> {
                alternatives.add(ChipUpgrades.REDUCE)
            }
            ChipType.DUP -> {
                alternatives.add(ChipUpgrades.REDUCE)
            }
            ChipType.ENTRY -> {}
            ChipType.CPU -> {}
        }

        // discard the alternatives that are not allowed for this stage,
        // but only for series 1.
        // In series 2 and 3, all upgrades are always possible
        if (network.theGame.currentStage.series == Game.SERIES_NORMAL) {
            val allowed = network.theGame.currentlyActiveStage?.data?.chipsAllowed ?: setOf()
            for (a in alternatives)
                if (a !in allowed)
                    alternatives.remove(a)
        }
        // discard the possibility for upgrade if the chip is already very high powered
        if (chipData.upgradeLevel >= 12)
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
        // gesture is not inside this chip
        {
            upgradePossibilities.clear()
            return false
        }
        if (chipData.type == ChipType.MEM)
            // activated MEM chips are cleared through tapping
        {
            if (isActivated() && !isInCooldown())
            {
                internalRegister.retrieve()
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
        /** this is an estimate how annoying an unwanted chip will be */
        val obstacleStrength =
            hashMapOf(
                ChipType.NOP to 0.5,
                ChipType.ADD to 1.0,
                ChipType.SHL to 1.2,
                ChipType.SPLT to 1.6,
                ChipType.DUP to 2.0)
        val obstacleTypes = obstacleStrength.keys

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

    inner class Register
    /** some chips have a 'register' where one or several attackers' value can be held. */
    {
        private var register = CopyOnWriteArrayList<Attacker>()

        fun clear()
        {
            register.clear()
        }

        fun store(attacker: Attacker)
        /** stores a value or a coin in the internal register, putting the attacker to HELD status */
        {
            register.add(attacker)
            attacker.gainCash()
            attacker.immuneTo = this@Chip
            attacker.data.state = Vehicle.State.HELD
            attacker.attackerData.storageNodeId = this@Chip.data.ident
            theNetwork.theGame.gameActivity.theGameView.theEffects?.fade(attacker)
        }

        fun retrieve(): Attacker?
        {
            try {
                return register.removeAt(0)
            }
            catch (ex: ArrayIndexOutOfBoundsException) // register is empty
            {
                return null
            }
        }

        fun releaseAll()
        /** releases the numbers stored in the register and make them move again */
        {
            register.forEach()
            {
                it.data.state = Vehicle.State.ACTIVE
                it.attackerData.storageNodeId = -1
                it.attackerData.hasNoValue = true
                it.jitterSpeed()
                it.makeNumber()
            }
            register.clear()
        }

        fun slotsUsed(): Int
        {
            return register.size
        }

        fun slotsFree(): Int
        {
            return slotsTotal() - slotsUsed()
        }

        fun slotsTotal(): Int
        {
            return when (chipData.type)
            {
                ChipType.ACC -> 1
                ChipType.MEM -> chipData.upgradeLevel
                else -> 0
            }
        }

        fun display(canvas: Canvas, rectOfChip: Rect)
        {
            val widthOfIndicator = rectOfChip.width() / Game.maxInternalChipStorage
            val indicatorRect = Rect(0,rectOfChip.bottom-rectOfChip.height()/4,widthOfIndicator,rectOfChip.bottom)
            for (i in 0 until slotsTotal())
            {
                indicatorRect.setBottomLeft(rectOfChip.left+i*widthOfIndicator,rectOfChip.bottom)
                // determine appearance of the indicator: solid, empty, or fading/coloured
                paintIndicator.alpha = 255
                paintIndicator.color = paintLines.color
                val indicatorsLit = slotsUsed()  // number of rectangles to be filled
                when (i)
                {
                    in 0 until indicatorsLit -> {
                        paintIndicator.style = Paint.Style.FILL
                        canvas.drawRect(indicatorRect, paintIndicator)
                    }
                    indicatorsLit -> {
                        if (isInCooldown())
                        {
                            paintIndicator.style = Paint.Style.FILL
                            // paintIndicator.color = theNetwork.theGame.resources.getColor(RES.color.chips_mem_foreground)
                            paintIndicator.alpha = (chipData.cooldownTimer*255f/getCooldownTime()).toInt()
                            canvas.drawRect(indicatorRect, paintIndicator)
                        }
                        paintIndicator.style = Paint.Style.STROKE
                        paintIndicator.alpha = 255
                        canvas.drawRect(indicatorRect, paintIndicator)
                    }
                    else -> {
                        paintIndicator.style = Paint.Style.STROKE
                        canvas.drawRect(indicatorRect, paintIndicator)
                    }
                }
            }
        }
    }
}
