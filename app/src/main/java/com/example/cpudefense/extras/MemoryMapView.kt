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

class MemoryMapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatImageView(context, attrs, defStyleAttr)
{
        companion object {
        val oneKiB = 1024
        val oneMiB = oneKiB*oneKiB
        val allowedSizes = listOf( 256, oneKiB, 4*oneKiB, 16*oneKiB, 64*oneKiB, oneMiB, 16*oneMiB, 64*oneMiB)
    }

    var summaryPerNormalLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerTurboLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerEndlessLevel = HashMap<Int, Stage.Summary>()
    var stageDataList: List<StageData> = listOf()
    var memoryBankList: List<MemoryBank> = listOf()
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
        memoryBankList = listOf( MemoryBank(Rect(0,0,width,height), totalCashSum/8) )
        memoryBankList[0].createMemTilesInBank()
    }

    fun colorOfStage(series: Int, number: Int): Pair<Int, Int>
    /** @return the pair of colour and border colour */
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
            memoryBankList[0].display(canvas)
            return bitmap
        }
        return null
    }

    inner class MemoryBank(val parentArea: Rect, val desiredMemSize: Int)
    {
        /** size in virtual 'bytes' of the memory bank */
        val memSize = allowedSizes.firstOrNull { it >= desiredMemSize } ?: allowedSizes.last()
        /** number of "memory words" (lines) in the bank */
        val bankSize = 256
        /** size of one line ('word') in bytes */
        val wordSize = memSize / bankSize
        /** the area of this bank on the screen */
        val area = Rect(parentArea.left, parentArea.top, parentArea.left+parentArea.width()/3, parentArea.bottom)

        var listOfTiles: MutableList<MemTile> = mutableListOf()


        fun createMemTilesInBank()
        {
            listOfTiles.clear()
            /** how many bytes will still fit into the last word */
            var infoRemainingInWord: Int = 0
            /** the "address" (that ist, the number of the line), counted from the bottom */
            var address = 0
            stageDataList.forEach {
                val color = colorOfStage(it.series, it.number)
                val bytes = it.amount / 8
                var infoRemaining = bytes
                if (infoRemainingInWord>0)  // first, fill up the word already begun
                    if (infoRemaining <= infoRemainingInWord)
                    {
                        listOfTiles.add(MemTile(this, bytes, color.first, color.second, address,
                                                wordSize-infoRemainingInWord, wordSize-infoRemainingInWord+infoRemaining))
                        infoRemainingInWord -= infoRemaining
                        infoRemaining = 0
                    }
                    else
                    {
                        listOfTiles.add(MemTile(this, bytes, color.first, color.second, address,
                                                wordSize-infoRemainingInWord, wordSize))
                        infoRemaining -= infoRemainingInWord
                        address += 1
                        infoRemainingInWord = 0
                    }
                // then, create entire lines
                repeat (infoRemaining/wordSize) {
                    listOfTiles.add(MemTile(this, bytes, color.first, color.second, address,
                                            0, wordSize))
                    address += 1
                }
                // finally, start a new "word" if there is a fraction left
                infoRemaining %= wordSize
                if (infoRemaining>0) {
                    listOfTiles.add(MemTile(this, bytes, color.first, color.second, address,
                                            0, infoRemaining))
                    infoRemainingInWord = wordSize - infoRemaining
                    if (infoRemainingInWord==0)
                        address += 1
                }
            }
        }

        fun display(canvas: Canvas)
        {
            /** height of one memory word in pixels */
            val scaleFactorHeight: Float = area.height() / bankSize.toFloat()
            /** width of one byte in pixels */
            val scaleFactorWidth: Float = area.width() / wordSize.toFloat()
            paintOutline.color = Color.WHITE
            canvas.drawRect(area, paintOutline)
            listOfTiles.forEach { tile ->
                paint.color = tile.color
                paintOutline.color = tile.borderColor
                val rect = tile.getRect(scaleFactorWidth, scaleFactorHeight)
                canvas.drawRect(rect, paint)
                // canvas.drawRect(rect, paintOutline)
            }
        }
    }

    inner class MemTile(val memoryBank: MemoryBank,
                        val amountInBytes: Int,
                        val color: Int,
                        val borderColor: Int,
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
            var amount: Int = 0,
    )

}