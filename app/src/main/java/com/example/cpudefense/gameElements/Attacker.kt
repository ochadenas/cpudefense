package com.example.cpudefense.gameElements

import android.graphics.*
import android.view.MotionEvent
import com.example.cpudefense.*
import com.example.cpudefense.effects.Explodable
import com.example.cpudefense.networkmap.Link
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Viewport
import kotlin.math.log2
import kotlin.random.Random


open class Attacker(network: Network, type: Representation = Representation.BINARY,
                    number: ULong = 1u, speed: Float = 1.0f):
    Vehicle(network), Explodable {
    enum class Representation { BINARY, HEX, DECIMAL, FLOAT }

    data class Data(
        val type: Representation,
        var number: ULong,
        var digits: Int,
        var bits: Int,
        var isCoin: Boolean = false,
        var vehicle: Vehicle.Data
    )

    companion object {
        fun makeNumber(attacker: Attacker): String
        {
            var text = attacker.attackerData.number.toString(radix=2).padStart(attacker.attackerData.digits, '0')
            attacker.createBitmap(text)
            return text
        }

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
            val attacker = Attacker(stage.network, data.type, data.number, data.vehicle.speed)
            attacker.data = data.vehicle
            attacker.attackerData = data
            attacker.attackerData = data
            attacker.data = data.vehicle
            attacker.onLink = stage.network.links[data.vehicle.linkId]
            attacker.startNode = stage.chips[data.vehicle.startNodeId]
            attacker.endNode = stage.chips[data.vehicle.endNodeId]
            attacker.onTrack = stage.tracks[data.vehicle.trackId]
            return attacker
        }
    }

    var attackerData = Data( type = type, number = number, digits = log2(number.toFloat()).toInt()+1,
        bits = 0, vehicle = super.data
    )
    var numberBitmap = Bitmap.createBitmap(100, 32, Bitmap.Config.ARGB_8888)
    var actualRect = Rect()
    var oldNumber: ULong = 0U
    var oldNumberBitmap: Bitmap? = null
    var animationCount = 0
    val animationCountMax = 8
    val numberFontSize = 24f
    var displacement = Pair(Random.nextInt(3)-1, Random.nextInt(5)-2) // small shift in display to avoid over-crowding on the screen

    init {
        if (attackerData.digits<1)
            attackerData.digits = 1
        else if (attackerData.digits>16)
            attackerData.digits = 16
        else while (! maskBinary.containsKey(attackerData.digits))
            attackerData.digits++  // adjust 'digits' to nearest allowed value
        attackerData.number = attackerData.number and maskBinary[attackerData.digits]!!
        makeNumber(this)
        attackerData.bits = attackerData.digits // TODO: change for hex values
        this.data.speed = speed
    }

    fun provideData(): Data
    {
        attackerData.vehicle.gridPos = posOnGrid?.asPair() ?: Pair(0f, 0f)
        /* get the ident of the current link */
        // val keys = theNetwork.links.filterValues { it == onLink }.keys
        attackerData.vehicle.linkId = onLink?.data?.ident ?: -1
        return attackerData
    }

    fun changeNumberTo(newNumber: ULong) {
        oldNumber = attackerData.number
        oldNumberBitmap = numberBitmap
        animationCount = animationCountMax
        attackerData.number = newNumber
        makeNumber(this)
    }

    fun invertNumber()
    {
        var n: ULong = attackerData.number
        n = n.inv()
        n = n and maskBinary[attackerData.digits]!!
        changeNumberTo(n)
    }

    open fun onShot(type: Chip.ChipType, power: Int): Boolean
            /** function that gets called when a the attacker gets "hit".
             * @param type the kind of attack, e. g. SUB
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
                    theNetwork.theGame.gameActivity.theGameView.theEffects?.explode(this)
                    theNetwork.theGame.scoreBoard.addScore(attackerData.bits)
                    return true
                }
                else
                    changeNumberTo(newNumber.toULong())
            }
            Chip.ChipType.SHIFT ->
            {
                for (i in 1 .. power)
                changeNumberTo((attackerData.number / 2u).toULong())
            }
            else -> return false
        }
        return false
    }

    override fun getPositionOnScreen(): Pair<Int, Int> {
        if (posOnGrid == null)
            return Pair(0, 0)
        else
            return theNetwork.theGame.viewport.gridToScreen(posOnGrid!!)
    }

    override fun remove()
    {
        if (onLink != null)
        {
            val link: Link = onLink!!
            link.node1.distanceToVehicle.remove(this)
            link.node2.distanceToVehicle.remove(this)
            theNetwork.vehicles.remove(this)
        }
    }

    fun createBitmap(text: String)
    {
        var textPaint = Paint()
        textPaint.textSize = numberFontSize
        textPaint.alpha = 255
        textPaint.color = theNetwork.theGame.resources.getColor(R.color.attackers_foreground)
        // paint.typeface = MONOSPACE
        textPaint.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD))
        textPaint.textAlign = Paint.Align.CENTER
        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        numberBitmap = Bitmap.createBitmap(bounds.width()+8, bounds.height()+10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(numberBitmap)
        var rect = Rect(0, 0, numberBitmap.width, numberBitmap.height)

        /* draw the actual (non-blurred) text */
        rect.displayTextCenteredInRect(canvas, text, textPaint)
        val alpha: Bitmap = numberBitmap.extractAlpha()

        /* create a transparent black background to have more contrast */
        val paint = Paint()
        paint.color = theNetwork.theGame.resources.getColor(R.color.attackers_background)
        // canvas.drawRect(rect, paint)

        /* use blurred image to create glow */
        val blurPaint = Paint()
        val blurMaskFilter = BlurMaskFilter(9f, BlurMaskFilter.Blur.OUTER)
        blurPaint.color = theNetwork.theGame.resources.getColor(R.color.attackers_glow)
        blurPaint.maskFilter = blurMaskFilter
        blurPaint.style = Paint.Style.FILL
        val blurCanvas = Canvas(numberBitmap)
        blurCanvas.drawBitmap(alpha, 0f, 0f, blurPaint)

        textPaint.color = theNetwork.theGame.resources.getColor(R.color.attackers_foreground)
        textPaint.maskFilter = null
        // rect.displayTextCenteredInRect(canvas, text, textPaint)
    }

    override fun update() {
        super.update()
        var link = onLink
        link?.node2?.notify(this, distanceToNextNode)
        link?.node1?.notify(this, -distanceFromLastNode)

        // animation, if any
        if (animationCount>0)
            animationCount--
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
        actualRect = Rect(0, 0, numberBitmap.width, numberBitmap.height)
        actualRect.setCenter(getPositionOnScreen())
        actualRect.offset(displacement.first, displacement.second)

        var paint = Paint()
        if (animationCount>0)
            oldNumberBitmap?.let {
                var divider = numberBitmap.height * animationCount / animationCountMax
                val newSource = Rect(0, 0, numberBitmap.width, numberBitmap.height-divider)
                val oldSource = Rect(0, numberBitmap.height-divider, numberBitmap.width, numberBitmap.height)
                val newTarget =  Rect(0, divider, numberBitmap.width, numberBitmap.height)
                newTarget.offsetTo(actualRect.left, actualRect.top+divider)
                val oldTarget = Rect(0, 0, numberBitmap.width, divider)
                oldTarget.offsetTo(actualRect.left, actualRect.top)
                canvas.drawBitmap(numberBitmap, newSource, newTarget, paint)
                canvas.drawBitmap(it, oldSource, oldTarget, paint)
        }
        else
            canvas.drawBitmap(numberBitmap, null, actualRect, paint)
    }

    fun onDown(event: MotionEvent): Boolean {
        val boundingRecSize = 50
        var boundingRect = Rect(0, 0, boundingRecSize, boundingRecSize)
        boundingRect.setCenter(actualRect.center())
        if (boundingRect.contains(event.x.toInt(), event.y.toInt())) // gesture is inside this object
        {
            invertNumber()
            return true
        }
        else
            return false
    }
}
