package com.example.bruiseapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import kotlin.random.Random

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val graphics: MutableList<Graphic> = mutableListOf()
    private val lock = Any()

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)

        fun scaleX(x: Float): Float = x * overlay.widthScaleFactor
        fun scaleY(y: Float): Float = y * overlay.heightScaleFactor

        fun translateX(x: Float): Float {
            return if (overlay.isFrontFacing) {
                overlay.width - scaleX(x)
            } else {
                scaleX(x)
            }
        }

        fun translateY(y: Float): Float = scaleY(y)
    }

    private val widthScaleFactor: Float
        get() = width.toFloat() / 480f // assuming preview width is 480
    private val heightScaleFactor: Float
        get() = height.toFloat() / 640f // assuming preview height is 640


    var isFrontFacing: Boolean = false

    fun clear() {
        synchronized(lock) {
            graphics.clear()
        }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) {
            graphics.add(graphic)
        }
    }

    fun addBruise(intensity: Float) {
        synchronized(lock) {
            val faceGraphic = graphics.firstOrNull { it is FaceGraphic } as? FaceGraphic
            faceGraphic?.let {
                val face = it.face
                val randomPoint = face.getContour(FaceContour.FACE)?.points?.randomOrNull()
                if (randomPoint != null) {
                    val bruise = Bruise(this, randomPoint.x, randomPoint.y, intensity)
                    add(bruise)
                }
            }
        }
        postInvalidate()
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}

class FaceGraphic(overlay: GraphicOverlay, val face: Face) : GraphicOverlay.Graphic(overlay) {

    private val facePositionPaint: Paint
    private val numColors = 10
    private val colors = Array(numColors) {
        intArrayOf(
            Color.BLACK,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
        ).random()
    }

    init {
        val selectedColor = Color.WHITE
        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor
    }

    override fun draw(canvas: Canvas) {
        val contour = face.getContour(FaceContour.FACE)
        if (contour != null) {
            val points = contour.points
            for (i in 0 until points.size - 1) {
                val startPoint = points[i]
                val endPoint = points[i + 1]
                canvas.drawLine(
                    translateX(startPoint.x),
                    translateY(startPoint.y),
                    translateX(endPoint.x),
                    translateY(endPoint.y),
                    facePositionPaint
                )
            }
            // Draw a line from the last point to the first point to close the contour
            if (points.isNotEmpty()) {
                val firstPoint = points.first()
                val lastPoint = points.last()
                canvas.drawLine(
                    translateX(lastPoint.x),
                    translateY(lastPoint.y),
                    translateX(firstPoint.x),
                    translateY(firstPoint.y),
                    facePositionPaint
                )
            }
        }
    }
}

class Bruise(
    overlay: GraphicOverlay,
    private val x: Float,
    private val y: Float,
    private val intensity: Float
) : GraphicOverlay.Graphic(overlay) {
    private val paint = Paint()
    private val size: Float

    init {
        paint.color = Color.argb(150, 102, 51, 153) // Purple-ish color
        paint.style = Paint.Style.FILL
        size = intensity * 0.5f
    }

    override fun draw(canvas: Canvas) {
        val scaledX = translateX(x)
        val scaledY = translateY(y)
        canvas.drawCircle(scaledX, scaledY, size, paint)
    }
}
