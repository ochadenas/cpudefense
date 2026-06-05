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
import com.example.cpudefense.utils.displayTextCenteredInRect
import com.example.cpudefense.utils.setBottomRight
import com.example.cpudefense.utils.setTopLeft
import kotlin.random.Random

class StatisticsMapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatImageView(context, attrs, defStyleAttr)
{
    var summaryPerNormalLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerTurboLevel  = HashMap<Int, Stage.Summary>()
    var summaryPerEndlessLevel = HashMap<Int, Stage.Summary>()
    var stageDataList: List<StageData> = listOf()
    /** total amount of cash, represented by the whole map */
    var totalCashSum = 0
    var listOfTiles: MutableList<MapTile> = mutableListOf()
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
        createTiles()
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
        listOfTiles.clear()
        activity.statisticsFragment.setCaption(totalCashSum)
    }

    fun createTiles()
    {
        var remainingArea = Rect(0, 0, width, height)
        stageDataList.sortedByDescending { it.amount }.takeWhile { it.amount > 1 }
            .forEach { summary ->
                val color = colorOfStage(summary.series, summary.number)
                val tile = MapTile(remainingArea, summary.amount.toFloat(),
                                   color.first, color.second,
                                   "%d:%d".format(summary.series, summary.number))
                listOfTiles.add(tile)
                remainingArea = tile.remainingArea
            }
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
            listOfTiles.forEach { tile ->
                paint.color = tile.color
                paintOutline.color = tile.borderColor
                canvas.drawRect(tile.area, paint)
                canvas.drawRect(tile.area, paintOutline)
                tile.area.displayTextCenteredInRect(canvas, tile.text, paintText)
            }
            return bitmap
        }
        return null
    }

    inner class MapTile(val parentArea: Rect, val amount: Float, val color: Int, val borderColor: Int, val text: String)
    {
        var area: Rect =
            let {
                var newWidth = parentArea.width()
                var newHeight = parentArea.height()
                val stretchFactor: Float = parentArea.width() / parentArea.height().toFloat() // greater than 1.0 if tile is stretched horizontally
                if (stretchFactor> Random.nextFloat()+0.5f)
                    newWidth = (amount / (this@StatisticsMapView.scaleFactor * parentArea.height())).toInt()
                else // area is stretched vertically
                    newHeight = (amount / (this@StatisticsMapView.scaleFactor * parentArea.width())).toInt()
                var newRect = Rect(0, 0, newWidth, newHeight)
                if (Random.nextBoolean())
                    newRect.setTopLeft(parentArea.left, parentArea.top)
                else
                    // newRect.setTopLeft(parentArea.left, parentArea.top)
                    newRect.setBottomRight(parentArea.right, parentArea.bottom)
            }

        var remainingArea: Rect =
            let {
                var newRect = Rect(parentArea)
                when
                {
                    area.left > parentArea.left -> newRect.right = area.left
                    area.right < parentArea.right -> newRect.left = area.right
                    area.top > parentArea.top -> newRect.bottom = area.top
                    area.bottom < parentArea.bottom -> newRect.top = area.bottom
                }
                newRect
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