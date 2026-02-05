package com.cat.emoji.fpf.maker.core.custom.paintview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Stack
import kotlin.math.abs

class PaintDrawView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private lateinit var path: Path
    private lateinit var paint: Paint
    private var currentColor = Color.BLACK
    private var currentStrokeWidth = 50f
    private var isEraser = false
    private lateinit var bitmap: Bitmap
    private lateinit var canvas: Canvas

    private var currentX = 0f
    private var currentY = 0f
    private val LIMIT_DISTANCE = 5
    private val pathList = Stack<DrawModel>()
    private var currentStrokeWidthEraser = 50f

    init {
        init()
    }

    private fun init() {
        paint = Paint()
        paint.color = currentColor
        paint.isAntiAlias = true
        paint.isDither = true
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = currentStrokeWidth
        paint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.save()
        c.drawBitmap(bitmap, 0f, 0f, Paint())
        canvas.drawColor(Color.WHITE)

        for (p in pathList) {
            if (!p.isEraser) {
                paint.color = p.color
            } else {
                paint.color = Color.WHITE
            }
            paint.strokeWidth = p.strokeWidth
            canvas.drawPath(p.path, paint)
        }
        c.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val targetX = event.x
        val targetY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path = Path()
                if (isEraser) {
                    pathList.push(DrawModel(path, currentColor, currentStrokeWidthEraser, isEraser))
                } else {
                    pathList.push(DrawModel(path, currentColor, currentStrokeWidth))
                }

                path.reset()
                path.moveTo(targetX, targetY)

                currentX = targetX
                currentY = targetY
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(targetX - currentX) >= LIMIT_DISTANCE || abs(targetY - currentY) >= LIMIT_DISTANCE) {
                    path.quadTo(currentX, currentY, (targetX + currentX) / 2, (targetY + currentY) / 2)
                    currentX = targetX
                    currentY = targetY
                }
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(currentX, currentY)
                invalidate()
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun eraser(state: Boolean) {
        isEraser = state
    }

    fun setStrokeWidthEraser(width: Int) {
        currentStrokeWidthEraser = width.toFloat()
    }

    fun getStrokeWidthEraser(): Int = currentStrokeWidthEraser.toInt()

    fun setColor(color: Int) {
        currentColor = color
    }

    fun setStrokeWidth(width: Int) {
        currentStrokeWidth = width.toFloat()
    }

    fun getStrokeWidth(): Int = currentStrokeWidth.toInt()

    fun save(): Bitmap? {
        return if (pathList.size > 0) {
            convertWhiteToTransparent(bitmap)
        } else {
            null
        }
    }

    private fun convertWhiteToTransparent(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)

                if (red == 255 && green == 255 && blue == 255) {
                    outputBitmap.setPixel(x, y, Color.TRANSPARENT)
                } else {
                    outputBitmap.setPixel(x, y, Color.argb(alpha, red, green, blue))
                }
            }
        }

        return outputBitmap
    }

    fun clearAll() {
        pathList.clear()
        if (::bitmap.isInitialized) {
            bitmap.eraseColor(Color.WHITE)
        }
        invalidate()
    }
}
