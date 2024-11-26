@file:Suppress("NOTHING_TO_INLINE", "unused")

package com.example.cpudefense.utils

import android.graphics.*

inline fun Rect.setCenter(x: Int, y: Int): Rect
{
    this.set(x-width()/2, y-height()/2, x+width()/2, y+height()/2)
    return this
}

inline fun Rect.setCenter(coord: Pair<Int, Int>): Rect
{
    this.setCenter(coord.first, coord.second)
    return this
}

inline fun Rect.setTop(y: Int): Rect
{
    this.set(left, y, right, y+height())
    return this
}

inline fun Rect.setLeft(x: Int): Rect
{
    this.set(x, top, x+width(), bottom)
    return this
}


inline fun Rect.setRight(x: Int): Rect
{
    this.set(x-width(), top, x, bottom)
    return this
}

inline fun Rect.setTopLeft(x: Int, y: Int): Rect
{
    this.set(x, y, x+width(), y+height())
    return this
}

inline fun Rect.setBottomRight(x: Int, y: Int): Rect
{
    this.set(x-width(), y-height(), x, y)
    return this
}

inline fun Rect.setBottomLeft(x: Int, y: Int): Rect
{
    this.set(x, y-height(), x+width(), y)
    return this
}

inline fun Rect.center(): Pair<Int, Int>
{
    return Pair(this.centerX(), this.centerY())
}

inline fun Rect.contains(coord: Pair<Int, Int>): Boolean
{
    return this.contains(coord.first, coord.second)
}


inline fun Rect.setRightEdge(x: Int, y: Int)
{
    this.set(x-width(), y-height()/2, x, y+height()/2)
}

inline fun Rect.scale(f: Float): Rect
        /** grows or shrinks the rectangle by a factor (in place)
         * @param f scale factor by which the rectangle is to be scaled. Values less than 1.0
         * means shrinking the rectangle.
         * @return the modified rectangle
         */
{
    val height = (this.height()*f).toInt()
    val width = (this.width()*f).toInt()
    this.set(centerX()-width/2, centerY()-height/2, centerX()+width/2, centerY()+height/2)
    return this
}

inline fun Rect.scaleAndSetCenter(coord: Pair<Int, Int>, f: Float): Rect
/** combined operation, avoids a function call */
{
    val x = coord.first
    val y = coord.second
    val height = (this.height()*f).toInt()
    val width = (this.width()*f).toInt()
    this.set(x-width/2, y-height/2, x+width/2, y+height/2)
    return this
}

inline fun Rect.inflate(amount: Int): Rect
        /** adds a fixed margin to each edge of the rectangle (in place)
         * @param amount inflate value (in pixels)
         * @return the modified rectangle
         */
{
    val height = (this.height()+2*amount)
    val width = (this.width()+2*amount)
    this.set(centerX()-width/2, centerY()-height/2, centerX()+width/2, centerY()+height/2)
    return this
}

inline fun Rect.shrink(amount: Int): Rect
        /** shrinks the rect at all 4 edges by the given value
         * @param amount shrink value (in pixels)
         * @return the modified rectangle
         */
{
    this.set(left+amount, top+amount, right-amount, bottom-amount)
    return this
}

inline fun Rect.makeSquare(): Rect
/**
 * makes the rectangle into a square that fits into the original rectangle (in place).
 * @return the modified rectangle
 */
{
    if (width()>height())
        this.set(centerX()-height()/2, top, centerX()+height()/2, bottom)
    else if (height()>width())
        this.set(left, centerY()-width()/2, right, centerY()+width()/2)
    return this
}

fun Rect.shiftBy(dx: Int, dy: Int): Rect
        /**
         * moves the rectangle by the offset given (in place).
         * @param dx offset in x direction
         * @param dy offset in y direction
         * @return the modified rectangle
         */
{
    setCenter(centerX()+dx, centerY()+dy)
    return this
}
private fun Rect.displayTextInRect(canvas: Canvas, text: String, paint: Paint, baseline: Int = 0, centered: Boolean, scaleToFit: Boolean): Rect
{
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    if (scaleToFit) {
        // scale down text if it doesn't fit:
        val excessSize = bounds.width().toFloat() / width().toFloat()
        if (excessSize > 1.0f) {
            paint.textSize /= excessSize
            paint.getTextBounds(text, 0, text.length, bounds)
        }
    }
    paint.textAlign = Paint.Align.LEFT
    val left = if (centered) this.centerX() - bounds.width()/2 else this.left
    val bottom = if (baseline>0) baseline else this.centerY() + bounds.height()/2
    canvas.drawText(text, left.toFloat(), bottom.toFloat(), paint)
    return Rect(left, bottom-bounds.height(), left+bounds.width(), bottom)
}

fun Rect.displayTextCenteredInRect(canvas: Canvas, text: String, paint: Paint, baseline: Int = 0): Rect
        /**
         * draws text centered to the center of this rectangle, using the actual text size.
         * @param text text to be drawn in the rectangle
         * @param paint paint that is used for the text
         * @param baseline if given, use as baseline for the text. Otherwise, center the text vertically.
         * @return the rectangle that bounds the text actually drawn
         */
{
    return this.displayTextInRect(canvas, text, paint, baseline, centered = true, scaleToFit = true)
}

fun Rect.displayTextLeftAlignedInRect(canvas: Canvas, text: String, paint: Paint, baseline: Int = 0): Rect
        /**
         * draws text left-aligned and centered to the vertical center of this rectangle, using the actual text size.
         * @param text text to be drawn in the rectangle
         * @param paint paint that is used for the text
         * @param baseline if given, use as baseline for the text. Otherwise, center the text vertically.
         * @return the rectangle that bounds the text actually drawn
         */
{
    return this.displayTextInRect(canvas, text, paint, baseline, centered = false, scaleToFit = true)
}

fun Rect.drawOutline(canvas: Canvas)
/**
 * for debugging purposes. Shows the rectangle by drawing a white outline around it
 */
{
    val paint = Paint()
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    canvas.drawRect(this, paint)
}

