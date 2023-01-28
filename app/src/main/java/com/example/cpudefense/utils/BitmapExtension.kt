package com.example.cpudefense.utils

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

// extension function to blur a bitmap
// from https://www.android--code.com/2020/06/android-kotlin-bitmap-blur-effect.html
fun Bitmap.blur(context: Context, radius:Float = 10F):Bitmap?
{
    val bitmap = copy(config,true)

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