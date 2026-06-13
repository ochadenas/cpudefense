package com.example.cpudefense.extras

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.createBitmap
import com.example.cpudefense.GameMechanics
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.Stage
import com.example.cpudefense.activities.ExtrasActivity
import com.example.cpudefense.gameElements.ScoreBoard
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setLeft
import com.example.cpudefense.utils.shrink

class MemoryMapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatImageView(context, attrs, defStyleAttr)
{
        companion object {
        const val ONE_KIB = 1024
        const val ONE_MIB = ONE_KIB*ONE_KIB
        const val ONE_GIB = ONE_MIB*ONE_KIB
        val allowedSizes = listOf(256, ONE_KIB, 4*ONE_KIB, 16*ONE_KIB, 64*ONE_KIB, 256*ONE_KIB,
                                  ONE_MIB, 4*ONE_MIB, 16*ONE_MIB, 64*ONE_MIB, 256*ONE_MIB,
                                  ONE_GIB, 4*ONE_GIB, 16*ONE_GIB, 64*ONE_GIB, 256*ONE_GIB, 1024*ONE_GIB)
    }

    var summaryPerNormalLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerTurboLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerEndlessLevel = HashMap<Int, Stage.Summary>()
    var stageDataList: List<StageData> = listOf()
    var memoryBankList: MutableList<MemoryBank> = mutableListOf()
    /** total amount of cash, represented by the whole map */
    var totalCashSum = 0
    /** the amount of cash that is represented by one pixel */
    var scaleFactor = 1.0f
    val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    val paintOutline = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
    }
    val paintText = Paint().apply {
        isAntiAlias = true
        textSize = resources.getDimension(R.dimen.statview_label_size)
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int)
    {
        super.onSizeChanged(w, h, oldw, oldh)
        updateStatisticsMap()
    }

    fun updateStatisticsMap() {
        if (width == 0 || height == 0) return
        loadSummaries()
        createBanks()
        setImageBitmap(createBitmapFromTiles())
    }

    fun loadSummaries()
    {
        val activity = context as? ExtrasActivity ?: return
        val persistency = Persistency(activity)
        summaryPerNormalLevel  =  persistency.loadStageSummaries(GameMechanics.SERIES_NORMAL)
        summaryPerTurboLevel  = persistency.loadStageSummaries(GameMechanics.SERIES_TURBO)
        summaryPerEndlessLevel = persistency.loadStageSummaries(GameMechanics.SERIES_ENDLESS)
        stageDataList =
            summaryPerNormalLevel.entries.map { (key, summary) ->
                StageData(series = GameMechanics.SERIES_NORMAL, number = key, amount = summary.totalCash) } +
            summaryPerTurboLevel.entries.map { (key, summary) ->
                StageData(series = GameMechanics.SERIES_TURBO, number = key, amount = summary.totalCash) } +
            summaryPerEndlessLevel.entries.map { (key, summary) ->
                    StageData(series = GameMechanics.SERIES_ENDLESS, number = key, amount = summary.totalCash) }
        totalCashSum = stageDataList.sumOf { it.amount }
        scaleFactor = totalCashSum / (width * height).toFloat()
        activity.statisticsFragment.setCaption(totalCashSum)
    }

    fun createBanks()
    {
        // determine appriopriate number and size of the banks to hold the info
        val maxNumberOfBanks = 4
        val infoInBytes = totalCashSum/8
        val requestedBytesPerBank = infoInBytes / maxNumberOfBanks
        val actualSizeOfBanks =  allowedSizes.firstOrNull { it >= requestedBytesPerBank } ?: allowedSizes.last()
        var actualNumberOfBanks = infoInBytes / actualSizeOfBanks + 1
        if (actualNumberOfBanks == 3) actualNumberOfBanks = 4 // avoid "ugly" numbers
        if (actualNumberOfBanks in (5..7)) actualNumberOfBanks = 8 // avoid "ugly" numbers
        val bankWidth = width/actualNumberOfBanks
        val bankRect = Rect(0, 0, bankWidth, height)
        repeat (actualNumberOfBanks) {index ->
            bankRect.setLeft(index*bankWidth)
            val memoryBank = MemoryBank(bankRect, actualSizeOfBanks)
            memoryBankList.add(memoryBank)
        }
        createMemTiles()
    }

    fun createMemTiles()
    {
        try {
            var index = 0
            memoryBankList[index].listOfTiles.clear()
            stageDataList.forEach {
                val color = colorOfStage(it.series, it.number)
                val remaining = memoryBankList[index].createMemTile(it.amount/8, color)
                if (remaining>0) {
                    index++
                    memoryBankList[index].createMemTile(remaining, color)
                }
            }
        } catch (_: IndexOutOfBoundsException) {}
    }

    /** Chooses a colour based on the stage identifier.
     * @return the pair of colour and border colour */
    fun colorOfStage(series: Int, number: Int): Pair<Int, Int>
    {
        var baseColor = Color.WHITE
        var borderColor = Color.WHITE
        when (series)
        {
            GameMechanics.SERIES_NORMAL -> {
                baseColor = resources.getColor(R.color.series_normal)
                borderColor = resources.getColor(R.color.series_normal_2)
            }
            GameMechanics.SERIES_TURBO -> {
                baseColor = resources.getColor(R.color.series_turbo)
                borderColor = resources.getColor(R.color.series_turbo_2)
            }
            GameMechanics.SERIES_ENDLESS -> {
                baseColor = resources.getColor(R.color.series_endless)
                borderColor = resources.getColor(R.color.series_endless_2)
            }
            else -> {}
        }
        // vary colour depending on stage number
        val red = Color.red(baseColor)
        val green = Color.green(baseColor)
        val blue = Color.blue(baseColor)
        val hsv = FloatArray(3)
        Color.RGBToHSV(red, green, blue, hsv)
        hsv[0] += (number % 11 - 5) * 4f // hue
        hsv[1] -= (number % 7) * 0.04f // saturation
        hsv[2] -= (number % 3) * 0.04f // brightness
        val varColor = Color.HSVToColor(hsv)
        return Pair(varColor, borderColor)
    }

    fun createBitmapFromTiles(): Bitmap? {
        if (width>0 && height>0) {
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            memoryBankList.forEach { it.display(canvas) }
            return bitmap
        }
        return null
    }

    /** represents a memory storage of a fixed size.
     * Shown on the screen as rectangle filled with coloured tiles.
     * @property area the area on the screen that the bank image takes
     * @property memSize number of bytes available in the bank */
    inner class MemoryBank(rect: Rect, val memSize: Int)
    {
        var area = Rect(rect).shrink(10)
        /** number of "memory words" (lines) in the bank */
        var bankSize = 64
        /** size of one line ('word') in bytes */
        var wordSize = memSize / bankSize
        var listOfTiles: MutableList<MemTile> = mutableListOf()

        /** the current "address" (that ist, the number of the line where new tiles go) */
        var address = 0
        /** how many bytes will still fit into the last word */
        var freeBytesRemainingInWord: Int = 0
        
        fun remainingWordsInBank(): Int
        {
            return bankSize-address
        }
        
        fun isFull(): Boolean
        { return remainingWordsInBank()<=0 }

        /** tries to put the desired amount of info into the bank (until it is full).
         * @param amount info in bytes
         * @param color pair of colours to be used for the tile
         * @return the number of bytes that must overflow into the next bank
         * if the memory bank is full
         */
        fun createMemTile(amount: Int, color: Pair<Int, Int>): Int
        {
            var infoRemaining = amount
            if (freeBytesRemainingInWord>0)  // first, fill up the word already begun
                if (infoRemaining <= freeBytesRemainingInWord) {
                    listOfTiles.add(MemTile(this, amount, color, address,
                                            wordSize-freeBytesRemainingInWord, wordSize-freeBytesRemainingInWord+infoRemaining))
                    freeBytesRemainingInWord -= infoRemaining
                    infoRemaining = 0
                }
                else
                {
                    listOfTiles.add(MemTile(this, amount, color, address,
                                            wordSize-freeBytesRemainingInWord, wordSize))
                    infoRemaining -= freeBytesRemainingInWord
                    address += 1
                    freeBytesRemainingInWord = 0
                }
            // then, create entire lines
            repeat (infoRemaining/wordSize) {
                if (isFull())
                    return infoRemaining
                listOfTiles.add(MemTile(this, amount, color, address,
                                        0, wordSize))
                address += 1
                infoRemaining -= wordSize
            }
            // finally, start a new "word" if there is a fraction left
            if (infoRemaining>0) {
                listOfTiles.add(MemTile(this, amount, color, address,
                                        0, infoRemaining))
                freeBytesRemainingInWord = wordSize - infoRemaining
                infoRemaining = 0
                if (freeBytesRemainingInWord==0)
                    address += 1
            }
            return infoRemaining
        }

        fun display(canvas: Canvas)
        {
            /** height of one memory word in pixels */
            val scaleFactorHeight: Float = area.height() / bankSize.toFloat()
            /** width of one byte in pixels */
            val scaleFactorWidth: Float = area.width() / wordSize.toFloat()
            paintOutline.color = Color.WHITE
            listOfTiles.forEach { tile ->
                paint.color = tile.color.first
                val rect = tile.getRect(scaleFactorWidth, scaleFactorHeight)
                canvas.drawRect(rect, paint)
            }
            canvas.drawRect(area, paintOutline)
            val textArea = Rect(area.left, area.top, area.right, area.top + area.height()/10)
            textArea.displayTextCenteredInRect(canvas, ScoreBoard.informationToString(memSize*8), paintText)
        }
    }

    inner class MemTile(val memoryBank: MemoryBank,
                        val amountInBytes: Int,
                        val color: Pair<Int, Int>,
                        val address: Int,
                        val startByte: Int,
                        val endByte: Int)
    /**
     * @param amountInBytes the whole amount of the stage, over all connected tiles
     * @param endByte is meant _inclusively_ */
    {
        fun getRect(scaleFactorWidth: Float, scaleFactorHeight: Float): Rect {
            if (endByte > memoryBank.wordSize)
                return Rect()
            val bottom = memoryBank.area.bottom - address * scaleFactorHeight
            val top = bottom - scaleFactorHeight
            val left = memoryBank.area.left + startByte * scaleFactorWidth
            val right = memoryBank.area.left + endByte * scaleFactorWidth
            return Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
        }
    }

    data class StageData
    /** a short form of the relevant stage data, including the identifier (series/number) */
    (
            var series: Int = 1,
            var number: Int = 1,
            /** info gained in this stage (in bits) */
            var amount: Int = 0,
    )

}