package com.example.cpudefense

import android.graphics.*

fun Rect.setCenter(x: Int, y: Int)
{
    this.set(x-width()/2, y-height()/2, x+width()/2, y+height()/2)
}

fun Rect.setCenter(coord: Pair<Int, Int>)
{
    this.setCenter(coord.first, coord.second)
}

fun Rect.setTop(y: Int)
{
    this.set(left, y, right, y+height())
}

fun Rect.setLeft(x: Int)
{
    this.set(x, top, x+width(), bottom)
}


fun Rect.setRight(x: Int)
{
    this.set(x-width(), top, x, bottom)
}

fun Rect.setTopLeft(x: Int, y: Int)
{
    this.set(x, y, x+width(), y+height())
}

fun Rect.setBottomRight(x: Int, y: Int)
{
    this.set(x-width(), y-height(), x, y)
}

fun Rect.center(): Pair<Int, Int>
{
    return Pair(this.centerX(), this.centerY())
}

fun Rect.contains(coord: Pair<Int, Int>): Boolean
{
    return this.contains(coord.first, coord.second)
}


fun Rect.setRightEdge(x: Int, y: Int)
{
    this.set(x-width(), y-height()/2, x, y+height()/2)
}

fun Rect.scale(f: Float): Rect
        /** grows or shrinks the rectangle by a factor (in place)
         * @param scale factor by which the rectangle is to be scaled. Values less than 1.0
         * means shrinking the rectangle.
         * @return the modified rectangle
         */
{
    val height = (this.height()*f).toInt()
    val width = (this.width()*f).toInt()
    this.set(centerX()-width/2, centerY()-height/2, centerX()+width/2, centerY()+height/2)
    return this
}

fun Rect.inflate(amount: Int): Rect
        /** adds a fixed margin to each edge of the rectangle (in place)
         * @param amount inflate value (in pixels)
         * @return the modified rectangle
         */
{
    val height = (this.height()+2*amount).toInt()
    val width = (this.width()+2*amount).toInt()
    this.set(centerX()-width/2, centerY()-height/2, centerX()+width/2, centerY()+height/2)
    return this
}

fun Rect.shrink(amount: Int): Rect
        /** shrinks the rect at all 4 edges by the given value
         * @param amount shrink value (in pixels)
         * @return the modified rectangle
         */
{
    this.set(left+amount, top+amount, right-amount, bottom-amount)
    return this
}

fun Rect.makeSquare(): Rect
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

fun Rect.offset(dx: Int, dy: Int): Rect
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

fun Rect.displayTextCenteredInRect(canvas: Canvas, text: String, paint: Paint): Rect
        /**
         * draws text centered to the center of this rectangle, using the actual text size.
         * @param text text to be drawn in the rectangle
         * @param paint paint that is used for the text
         * @returns the rectangle that bounds the text actually drawn
         */
{
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)
    paint.textAlign = Paint.Align.LEFT
    val rect = Rect(this.centerX() - bounds.width()/2, this.centerY() + bounds.height()/2, this.centerX() + bounds.width()/2, this.centerY() - bounds.height()/2)
    canvas.drawText(text, rect.left.toFloat(), rect.top.toFloat(), paint)
    return rect
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

