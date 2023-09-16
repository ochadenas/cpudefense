package com.example.cpudefense.gameElements

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.R
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setTopLeft

class SevenSegmentDisplay(val numberOfDigits: Int, val size: Int, val activity: Activity): GameElement()
{
    enum class LedColors { GREEN, RED, YELLOW, WHITE }
    var digit = List(numberOfDigits) {0}
    var resources = activity.resources

    val mask = listOf<Bitmap>(
        decodeResource(resources, R.drawable.digit_0),
        decodeResource(resources, R.drawable.digit_1),
        decodeResource(resources, R.drawable.digit_2),
        decodeResource(resources, R.drawable.digit_3),
        decodeResource(resources, R.drawable.digit_4),
        decodeResource(resources, R.drawable.digit_5),
        decodeResource(resources, R.drawable.digit_6),
        decodeResource(resources, R.drawable.digit_7),
        decodeResource(resources, R.drawable.digit_8),
        decodeResource(resources, R.drawable.digit_9)
    )
    val backgroundLight = hashMapOf<LedColors, Bitmap>(
        LedColors.GREEN  to decodeResource(resources, R.drawable.led_green),
        LedColors.YELLOW to decodeResource(resources, R.drawable.led_yellow),
        LedColors.RED    to decodeResource(resources, R.drawable.led_red),
        LedColors.WHITE  to decodeResource(resources, R.drawable.led_white)
    )

    val naturalHeight = mask[0].height
    val naturalWidth = mask[0].width
    val margin = 2

    val sizeY = size
    val scale = sizeY / naturalHeight.toFloat()
    val sizeX = (scale * naturalWidth).toInt()

    val paint = Paint()
    val paintBorder = Paint()

    fun getDisplayBitmap(number: Int, ledColor: LedColors): Bitmap
    {
        val bitmap = Bitmap.createBitmap(numberOfDigits*sizeX+2*margin, sizeY+2*margin, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        paintBorder.style = Paint.Style.STROKE
        paintBorder.color = activity.resources.getColor(R.color.design_default_color_primary)
        val rectBorder = Rect(0,0,bitmap.width-margin, bitmap.height-margin)
        canvas.drawRect(rectBorder, paintBorder)
        var destRect = Rect(0,0,sizeX,sizeY)
        var n = number
        for (d in numberOfDigits-1 downTo  0)
        {
            val digit = n % 10
            n /= 10
            destRect.setTopLeft(margin+d*sizeX, margin)
            canvas.drawBitmap(backgroundLight[ledColor]!!, null, destRect, paint)
            canvas.drawBitmap(mask[digit], null, destRect, paint)
        }
        return bitmap
    }


    override fun update() {
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
    }

}