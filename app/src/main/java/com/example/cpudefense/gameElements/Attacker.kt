@file:Suppress("DEPRECATION")

package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Explodable
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.networkmap.*
import com.example.cpudefense.utils.*
import kotlin.math.exp
import kotlin.math.log2
import kotlin.random.Random


open class Attacker(network: Network, representation: Representation = Representation.BINARY,
                    number: ULong = 1u, speed: Float = 1.0f):
    Vehicle(network), Explodable, Fadable {
    enum class Representation { UNDEFINED, BINARY, HEX, DECIMAL, FLOAT }

    data class Data(
        var representation: Representation,
        var number: ULong,
        var binaryDigits: Int,
        var hexDigits: Int,
        var bits: Int,
        var isCoin: Boolean = false,
        var vehicle: Vehicle.Data,
        /** flag that is set if destruction of this attacker must not yield cash */
        var hasNoValue: Boolean = false,
        /** remember if the attacker is held in a chip */
        var storageNodeId: Int = -1,
    )

    var attackerData = Data( representation = representation, number = number, binaryDigits = 0, hexDigits = 0,
        bits = 0, vehicle = super.data
    )
    private val resources = network.gameView.resources
    private val activity = network.gameView.gameMechanics.gameActivity
    private var numberBitmap: Bitmap = Bitmap.createBitmap(100, 32, Bitmap.Config.ARGB_8888)
    var actualRect = Rect()
    private var oldNumber: ULong = 0U
    private var oldNumberBitmap: Bitmap? = null
    var immuneTo: Chip? = null
    var immuneToAll = false
    var animationCount = 0
    private val animationCountMax = 8
    private var baseNumberFontSize = 32f
    private var numberFontSize = baseNumberFontSize  // must be scaled
    var displacement = Pair(Random.nextInt(5)-1, Random.nextInt(7)-2) // small shift in display to avoid over-crowding on the screen
    private val paintBitmap = Paint()
    private var scale: Float = 1.0f

    init {
        this.data.speed = speed
        if (attackerData.bits == 0)
            calculateNumberOfDigits()
    }

    fun copy(): Attacker
    /** creates a deep copy of the attacker object */
    {
        val newAttacker = Attacker(network = network, representation = attackerData.representation, number = attackerData.number, speed = data.speed )
        newAttacker.data = data.copy()
        newAttacker.attackerData = attackerData.copy()
        newAttacker.onTrack = onTrack
        newAttacker.actualRect = Rect(actualRect)
        newAttacker.scale = scale
        newAttacker.posOnGrid = posOnGrid
        newAttacker.onLink = onLink
        newAttacker.startNode = startNode
        newAttacker.endNode= endNode
        newAttacker.distanceFromLastNode = distanceFromLastNode
        newAttacker.distanceToNextNode = distanceToNextNode
        newAttacker.makeNumber()
        return newAttacker
    }


    fun provideData(): Data
    /** copies some transient variables into the persistent attacker data.
     * Used in the stage's provideData function.
     * @return the data object */
    {
        attackerData.vehicle.gridPos = posOnGrid?.asPair() ?: Pair(0f, 0f)
        /* get the ident of the current link */
        // val keys = theNetwork.links.filterValues { it == onLink }.keys
        attackerData.vehicle.linkId = onLink?.data?.ident ?: -1
        return attackerData
    }

    private fun calculateNumberOfDigits()
    /** determine how many binary or hex digits the value must have,
     * given its number
     */
    {
        if (attackerData.representation == Representation.UNDEFINED)
            attackerData.representation = if (attackerData.number >= 32u) Representation.HEX else Representation.BINARY
        if (attackerData.representation == Representation.BINARY)
        {
            attackerData.binaryDigits = log2(attackerData.number.toFloat()).toInt() + 1
            if (attackerData.binaryDigits < 1)
                attackerData.binaryDigits = 1
            else if (attackerData.binaryDigits > 16)
                attackerData.binaryDigits = 16
            else while (!maskBinary.containsKey(attackerData.binaryDigits))
                attackerData.binaryDigits++  // adjust 'digits' to nearest allowed value
            maskBinary[attackerData.binaryDigits]?.let { mask ->
                attackerData.number = attackerData.number and mask }
        }
        else
        {
            attackerData.hexDigits = log16(attackerData.number.toFloat()).toInt() + 1
            if (attackerData.hexDigits < 2)
                attackerData.hexDigits = 2
            else if (attackerData.hexDigits > 8)
                attackerData.hexDigits = 8
            else while (!maskHex.containsKey(attackerData.hexDigits))
                attackerData.hexDigits++  // adjust 'digits' to nearest allowed value
            maskHex[attackerData.hexDigits]?.let { mask ->
                attackerData.number = attackerData.number and mask }
        }
        attackerData.bits = attackerData.binaryDigits + 4 * attackerData.hexDigits
    }

    fun changeNumberTo(newNumber: ULong)
    /** change the attacker's number to a new number,
     * displaying an animation.
     */
    {
        oldNumber = attackerData.number
        oldNumberBitmap = numberBitmap
        animationCount = animationCountMax
        attackerData.number = newNumber
        if (newNumber>oldNumber)
            calculateNumberOfDigits()
        makeNumber()
    }

    private fun invertNumber()
    /** turns the number into its binary complement */
    {
        var n: ULong = attackerData.number
        n = n.inv()
        if (attackerData.representation == Representation.BINARY)
            maskBinary[attackerData.binaryDigits]?.let { mask -> n = n and mask }
        else
            maskHex[attackerData.hexDigits]?.let { mask -> n = n and mask }
        changeNumberTo(n)
    }

    private fun extraCash(): Int
    /** @return possible bonus on kill due to hero */
    {
        val strength = network.gameMechanics.heroModifier(Hero.Type.GAIN_CASH_ON_KILL)
        val extraCash = Random.nextFloat() * strength * 2.0f // this gives an expectation value of 'strength'
        return extraCash.toInt()
    }

    fun gainCash()
    /** credit the info gained for eliminating this attacker */
    {
        if (attackerData.hasNoValue == false)
            network.gameView.scoreBoard.addCash(attackerData.bits + extraCash())
    }

    fun slowDown(modifier: Float)
    /** temporarily decrease the attacker's speed, e.g. due to a resistor effect */
    {
        data.speedModifier = modifier
        var additionalDuration = GameMechanics.resistorBaseDuration / data.speedModifier * network.gameMechanics.heroModifier(Hero.Type.INCREASE_CHIP_RES_DURATION)
        if (additionalDuration > GameMechanics.resistorMaxDuration)
            additionalDuration = GameMechanics.resistorMaxDuration
        data.speedModificationTimer += additionalDuration
        makeNumber()
    }

    fun effectOfResistanceOnSpeed(ohm: Float): Float
    /** speed modification caused by a resistor
     * @param ohm Resistance value
     */
    {
        return exp(- (ohm*attackerData.bits) / 320.0f)
    }


    open fun onShot(type: Chip.ChipType, power: Int): Boolean
    /** function that gets called when a the attacker gets "hit".
     * @param type the chip's type that effectuates the attack
     * @param power strength (amount) of the shot
     * @return true if the attacker gets destroyed, false otherwise
     */
    {
        when (type)
        {
            Chip.ChipType.SUB ->
            {
                val newNumber =  attackerData.number.toLong() - power
                if (newNumber < 0)
                {
                    network.gameMechanics.gameActivity.gameView.effects?.explode(this)
                    gainCash()
                    return true
                }
                else
                    changeNumberTo(newNumber.toULong())
            }
            Chip.ChipType.SHR ->
            {
                val factor: UInt = powerOfTwo[power] ?: 1u
                changeNumberTo((attackerData.number / factor))
            }
            Chip.ChipType.MEM ->
            {
                // theNetwork.theGame.scoreBoard.addCash(attackerData.bits + extraCashGained())
            }
            Chip.ChipType.ADD ->
            {
                val newNumber =  attackerData.number.toLong() + power
                changeNumberTo(newNumber.toULong())
            }
            Chip.ChipType.SHL ->
            {
                val factor: UInt = powerOfTwo[power] ?: 1u
                changeNumberTo((attackerData.number * factor))

            }
            Chip.ChipType.NOP ->
            {
                // does nothing
            }
            else -> return false
        }
        return false
    }

    override fun getPositionOnScreen(): Pair<Int, Int>
    /** given grid coordinates, calculate the actual pixel coordinates.
     * @return The position as pair of pixels (x, y),
     * or (0, 0) if the viewport is undefined or invalid.
     */
    {
        posOnGrid?.let { return network.gameView.viewport.gridToViewport(it) }
        /* else, if posOnGrid == null: */
        return Pair(0, 0)
    }

    override val explosionColour: Int?
        get() = when (attackerData.representation)
        {
            Representation.UNDEFINED -> null
            Representation.BINARY -> resources.getColor(R.color.attackers_glow_bin)
            Representation.HEX -> resources.getColor(R.color.attackers_glow_hex)
            Representation.DECIMAL -> null
            Representation.FLOAT -> null
        }

    override fun remove()
    /** removes this attacker from the network */
    {
        onLink?.let {
            it.node1.notify(this, direction = Node.VehicleDirection.GONE)
            it.node2.notify(this, direction = Node.VehicleDirection.GONE)
            data.state = State.GONE
            network.vehicles.remove(this) // TODO: this might not be thread safe
        }
    }

    open fun makeNumber()
    /** creates a bitmap using the current number (strength) of the attacker.
     * N.B.: Cryptocoins have their own implementation of this method.
     */
    {
        val text: String
        if (attackerData.representation == Representation.BINARY)
            text = attackerData.number.toString(radix=2).padStart(attackerData.binaryDigits, '0')
        else
            text = "x" + attackerData.number.toString(radix=16).uppercase().padStart(attackerData.hexDigits, '0')
        createBitmap(text)
        scale = 1.0f  // reset any shrinking effects
    }

    fun createBitmap(text: String)
    {
        // determine size
        numberFontSize = baseNumberFontSize * network.gameView.textScaleFactor *
                if (activity.settings.configUseLargeButtons) 1.5f else 0.8f
        // define colours
        val textPaint = Paint()
        val blurPaint = Paint()
        val blurMaskFilter = BlurMaskFilter(11f, BlurMaskFilter.Blur.OUTER)
        when (attackerData.representation) {
            Representation.BINARY -> {
                if (data.speedModificationTimer > 0)
                    textPaint.color = resources.getColor(R.color.attackers_slowed_bin)
                else
                    textPaint.color = resources.getColor(R.color.attackers_foreground_bin)
                blurPaint.color = resources.getColor(R.color.attackers_glow_bin)
            }
            else -> {
                if (data.speedModificationTimer > 0)
                    textPaint.color = resources.getColor(R.color.attackers_slowed_hex)
                else
                    textPaint.color = resources.getColor(R.color.attackers_foreground_hex)
                blurPaint.color = resources.getColor(R.color.attackers_glow_hex)
            }
        }

        textPaint.textSize = numberFontSize
        textPaint.alpha = 255
        textPaint.typeface = network.gameView.boldTypeface
        textPaint.textAlign = Paint.Align.CENTER
        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        numberBitmap = Bitmap.createBitmap(bounds.width()+8, bounds.height()+10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(numberBitmap)
        val rect = Rect(0, 0, numberBitmap.width, numberBitmap.height)

        /* draw the actual (non-blurred) text */
        rect.displayTextCenteredInRect(canvas, text, textPaint)
        val alpha: Bitmap = numberBitmap.extractAlpha()

        /* create a transparent black background to have more contrast */
        val paint = Paint()
        paint.color = resources.getColor(R.color.attackers_background)
        // canvas.drawRect(rect, paint)

        /* use blurred image to create glow */
        blurPaint.maskFilter = blurMaskFilter
        blurPaint.style = Paint.Style.FILL
        val blurCanvas = Canvas(numberBitmap)
        blurCanvas.drawBitmap(alpha, 0f, 0f, blurPaint)

        textPaint.color = resources.getColor(R.color.attackers_foreground_bin)
        textPaint.maskFilter = null
    }

    override fun update() {
        super.update()
        endNode?.notify(this, distanceToNextNode, Node.VehicleDirection.APPROACHING)
        startNode?.notify(this, distanceFromLastNode, Node.VehicleDirection.LEAVING)

        when
        {
            data.speedModificationTimer > 0 ->
            { data.speedModificationTimer -= network.gameMechanics.globalSpeedFactor() }
            data.speedModificationTimer < 0 ->
            {
                data.speedModificationTimer = 0.0f
                data.speedModifier = 0.0f
                if (data.state != State.HELD)
                    makeNumber()
            }
        }


        // animation, if any
        if (animationCount>0)
            animationCount--
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        if (posOnGrid == null)
            return
        actualRect = Rect(0, 0, numberBitmap.width, numberBitmap.height)
        actualRect.scaleAndSetCenter(getPositionOnScreen(), scale)
        actualRect.offset(displacement.first, displacement.second)

        if (animationCount>0)
            oldNumberBitmap?.let {
                val divider = numberBitmap.height * animationCount / animationCountMax
                val newSource = Rect(0, 0, numberBitmap.width, numberBitmap.height-divider)
                val oldSource = Rect(0, numberBitmap.height-divider, numberBitmap.width, numberBitmap.height)
                val newTarget =  Rect(0, divider, numberBitmap.width, numberBitmap.height)
                newTarget.offsetTo(actualRect.left, actualRect.top+divider)
                val oldTarget = Rect(0, 0, numberBitmap.width, divider)
                oldTarget.offsetTo(actualRect.left, actualRect.top)
                canvas.drawBitmap(numberBitmap, newSource, newTarget, paintBitmap)
                canvas.drawBitmap(it, oldSource, oldTarget, paintBitmap)
        }
        else
            canvas.drawBitmap(numberBitmap, null, actualRect, paintBitmap)
    }

    override fun fadeDone(type: Fader.Type) {
        scale = 0.0f
    }

    override fun setOpacity(opacity: Float) {
        scale = opacity
    }

    fun jitterSpeed()
    /** applies a small change to the attacker's speed */
    {
        val changeInSpeed = data.speed * (Random.nextFloat() - 0.5f) * 0.3f
        data.speed += changeInSpeed
    }

    fun onDown(event: MotionEvent): Boolean {
        val boundingRect = Rect(actualRect)
        boundingRect.inflate(20).setCenter(actualRect.center())
        if (boundingRect.contains(event.x.toInt(), event.y.toInt())) // gesture is inside this object
        {
            invertNumber()
            return true
        }
        else
            return false
    }


    companion object
    {

        fun log16(v: Float): Float
        {
            val l16 = log2(16f)
            return log2(v) / l16
        }

        val powerOfTwo: HashMap<Int, UInt> = hashMapOf(
            0 to 1u, 1 to 2u, 2 to 4u, 3 to 8u, 4 to 16u,
            5 to 32u, 6 to 64u, 7 to 128u, 8 to 256u,
            9 to 512u, 10 to 1024u, 11 to 2048u, 12 to 4096u,
            13 to 8192u, 14 to 16384u, 15 to 32768u, 16 to 65536u,
            17 to 131072u, 18 to 262144u, 19 to 524288u, 20 to 1048576u
        )

        val maskBinary: HashMap<Int, ULong> = hashMapOf(
            1 to 0x01uL, 2 to 0x03uL,  4 to 0x0FuL,
            6 to 0x3FuL, 8 to 0xFFuL,
            12 to 0x0FFFuL, 16 to 0x7FFFuL, 32 to 0xFFFFuL, 64 to 0xFFFFFFFFuL )

        val maskHex: HashMap<Int, ULong> = hashMapOf(
            1 to 0x0FuL, 2 to 0xFFuL,  4 to 0xFFFFuL,
            6 to 0xFFFFFFuL, 8 to 0xFFFFFFuL,
            12 to 0xFFFFFFFFFFFFuL, 16 to 0xFFFFFFFFFFFFFFFFuL)

        fun createFromData(stage: Stage, data: Data): Attacker
        {
            val attacker = if (data.isCoin)
                Cryptocoin(stage.network, data.number, data.vehicle.speed)
            else
                Attacker(stage.network, data.representation, data.number, data.vehicle.speed)
            attacker.data = data.vehicle
            attacker.attackerData = data
            attacker.onTrack = stage.tracks[data.vehicle.trackId]
            val link = stage.network.links[data.vehicle.linkId]
            link?.let {
                val startNode = stage.chips[data.vehicle.startNodeId]
                attacker.setOntoLink(it, startNode)
                attacker.setCurrentDistanceOnLink(it)
                attacker.setPositionOnGrid(it, startNode)
            }
            attacker.makeNumber()
            if (attacker.data.state == State.HELD)
            {
                val storage: Chip? = stage.chips[attacker.attackerData.storageNodeId]
                storage?.internalRegister?.store(attacker)
            }
            return attacker
        }
    }
}
