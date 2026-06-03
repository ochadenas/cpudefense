package com.example.cpudefense.extras

import android.app.Activity
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
import com.example.cpudefense.utils.setTopLeft

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
        isAntiAlias = true
        style = Paint.Style.FILL
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
                val color = (Math.random() * 16777215).toInt() or (0xFF shl 24)
                val tile = MapTile(remainingArea, summary.amount.toFloat(), color, "%d:%d".format(summary.series, summary.number))
                listOfTiles.add(tile)
                remainingArea = tile.remainingArea
            }
    }

    fun createBitmapFromTiles(): Bitmap? {
        if (width>0 && height>0) {
            val bitmap = createBitmap(width, height)
            val canvas = Canvas(bitmap)
            listOfTiles.forEach { tile ->
                paint.color = tile.color
                canvas.drawRect(tile.area, paint)
                tile.area.displayTextCenteredInRect(canvas, tile.text, paintText)
            }
            return bitmap
        }
        return null
    }

    inner class MapTile(val parentArea: Rect, val amount: Float, val color: Int, val text: String)
    {
        var area: Rect =
            let {
                var newWidth = parentArea.width()
                var newHeight = parentArea.height()
                if (parentArea.width() > parentArea.height())  // area is streched horizontally
                    newWidth = (amount / (this@StatisticsMapView.scaleFactor * parentArea.height())).toInt()
                else // area is stretched vertically
                    newHeight = (amount / (this@StatisticsMapView.scaleFactor * parentArea.width())).toInt()
                Rect(0, 0, newWidth, newHeight).setTopLeft(parentArea.left, parentArea.top)
            }

        var remainingArea: Rect =
            let {
                var newRect = Rect(parentArea)
                if (area.width() < parentArea.width())
                    newRect.left = area.right
                else if (area.height() < parentArea.height())
                    newRect.top = area.bottom
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