package com.example.cpudefense.gameElements

import android.app.Activity
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.example.cpudefense.R
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setTopLeft
import androidx.core.graphics.createBitmap

open class SevenSegmentDisplay(
    /** the (fixed) number of decimal or hex digits in the display */
    private val numberOfDigits: Int,
    /** desired height in dp. Must include the display density */
    val size: Int,
    /** required to access resources */
    activity: Activity)
    : GameElement()
{
    enum class LedColors { GREEN, RED, YELLOW, WHITE }
    private var resources: Resources = activity.resources
    private val casingMask: Bitmap = decodeResource(resources, R.drawable.mask)
    private val digitMask = listOf<Bitmap>(
        decodeResource(resources, R.drawable.digit_0),
        decodeResource(resources, R.drawable.digit_1),
        decodeResource(resources, R.drawable.digit_2),
        decodeResource(resources, R.drawable.digit_3),
        decodeResource(resources, R.drawable.digit_4),
        decodeResource(resources, R.drawable.digit_5),
        decodeResource(resources, R.drawable.digit_6),
        decodeResource(resources, R.drawable.digit_7),
        decodeResource(resources, R.drawable.digit_8),
        decodeResource(resources, R.drawable.digit_9),
        decodeResource(resources, R.drawable.digit_a),
        decodeResource(resources, R.drawable.digit_b),
        decodeResource(resources, R.drawable.digit_c),
        decodeResource(resources, R.drawable.digit_d),
        decodeResource(resources, R.drawable.digit_e),
        decodeResource(resources, R.drawable.digit_f)
    )
    private val dotMask = decodeResource(resources, R.drawable.dot_mask)
    /** a second array for all digit mask, but additionally with the dot cut out  */
    private val digitMaskWithDot: List<Bitmap> = digitMask.map { bitmap ->
        val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)
        val paintMask = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
        canvas.drawBitmap(dotMask, 0f, 0f, paintMask)
        resultBitmap
    }
    private val backgroundLight = hashMapOf<LedColors, Bitmap>(
        LedColors.GREEN  to decodeResource(resources, R.drawable.led_green),
        LedColors.YELLOW to decodeResource(resources, R.drawable.led_yellow),
        LedColors.RED    to decodeResource(resources, R.drawable.led_red),
        LedColors.WHITE  to decodeResource(resources, R.drawable.led_white)
    )

    private val naturalHeight = digitMask[0].height
    private val naturalWidth = digitMask[0].width
    private val margin = 2

    private val sizeY = size
    private val scale = sizeY / naturalHeight.toFloat()
    private val sizeX = (scale * naturalWidth).toInt()

    val paint = Paint()

    override fun update() {
    }

    open fun getDisplayBitmap(number: Int, ledColor: LedColors, isLit: Boolean = true, radix: Int = 10,
                              dotPosition: List<Int> = listOf()): Bitmap
            /** creates the display for the bitmap.
             * @param dotPosition a list of positions that have decimal dots lit
             */
    {
        val bitmap = createBitmap(numberOfDigits * sizeX + 2 * margin, sizeY + 2 * margin)
        if ((radix <= 0) || (radix > 16))
            return bitmap // value not allowed. In fact, only 10 or 16 are reasonable bases
        val canvas = Canvas(bitmap)
        val destRect = Rect(0, 0, sizeX, sizeY)
        var n = number
        for (d in numberOfDigits-1 downTo  0)
        {
            val digit = n % radix
            n /= radix
            destRect.setTopLeft(margin+d*sizeX, margin)
            if (isLit) {
                canvas.drawBitmap(backgroundLight[ledColor]!!, null, destRect, paint)
                if (d in dotPosition)
                    canvas.drawBitmap(digitMaskWithDot[digit], null, destRect, paint)
                else
                    canvas.drawBitmap(digitMask[digit], null, destRect, paint)
            }
            canvas.drawBitmap(casingMask, null, destRect, paint)
        }
        return bitmap
    }

    override fun display(canvas: Canvas, viewport: Viewport) {
    }

}