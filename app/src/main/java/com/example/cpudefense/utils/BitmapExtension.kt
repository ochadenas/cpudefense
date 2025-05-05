@file:Suppress("DEPRECATION")

package com.example.cpudefense.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import android.graphics.Color
import android.graphics.Matrix
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

// extension function to blur a bitmap
// from https://www.android--code.com/2020/06/android-kotlin-bitmap-blur-effect.html
fun Bitmap.blur(context: Context, radius: Float = 10F): Bitmap?
/** extension function to blur a bitmap
 * from https://www.android--code.com/2020/06/android-kotlin-bitmap-blur-effect.html
 * */
{
    val bitmap = copy(config ?: ARGB_8888,true)

    RenderScript.create(context).apply {
        val input = Allocation.createFromBitmap(this,this@blur)
        val output = Allocation.createFromBitmap(this,this@blur)

        ScriptIntrinsicBlur.create(this, Element.U8_4(this)).apply {
            setInput(input)
            // Set the radius of the Blur. Supported range 0 < radius <= 25
            setRadius(radius)
            forEach(output)
            output.copyTo(bitmap)
            destroy()
        }
    }
    return bitmap
}

fun Bitmap.flipHorizontally(): Bitmap
        /** flips the bitmap horizontally. Taken from
         * https://stackoverflow.com/questions/36493977/flip-a-bitmap-image-horizontally-or-vertically
         */
{
    val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flipVertically(): Bitmap
        /** flips the bitmap vertically. */
{
    val matrix = Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.clear()
/** just a function to make the naming clearer */
{
    eraseColor(Color.TRANSPARENT)
}