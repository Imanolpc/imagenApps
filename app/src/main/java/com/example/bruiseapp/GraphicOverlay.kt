package com.example.bruiseapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import kotlin.random.Random

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val graphics: MutableList<Graphic> = mutableListOf()
    private val bruises: MutableList<Bruise> = mutableListOf()
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
            if (graphics.isNotEmpty()) {
                val faceGraphic = graphics.firstOrNull { it is FaceGraphic } as? FaceGraphic
                faceGraphic?.let {
                    val face = it.face
                    val randomPoint = face.allPoints.randomOrNull()
                    if (randomPoint != null) {
                        bruises.add(Bruise(this, randomPoint.position.x, randomPoint.position.y, intensity))
                    }
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
            for (bruise in bruises) {
                bruise.draw(canvas)
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
        val face = this.face ?: return

        val x = translateX(face.boundingBox.centerX().toFloat())
        val y = translateY(face.boundingBox.centerY().toFloat())

        // Draws a bounding box around the face.
        val xOffset = scaleX(face.boundingBox.width() / 2.0f)
        val yOffset = scaleY(face.boundingBox.height() / 2.0f)
        val left = x - xOffset
        val top = y - yOffset
        val right = x + xOffset
        val bottom = y + yOffset
        canvas.drawRect(left, top, right, bottom, facePositionPaint)
    }
}

class Bruise(
    private val overlay: GraphicOverlay,
    private val x: Float,
    private val y: Float,
    private val intensity: Float
) {
    private val paint = Paint()
    private val size: Float

    init {
        paint.color = Color.argb(150, 102, 51, 153) // Purple-ish color
        paint.style = Paint.Style.FILL
        size = intensity * 0.5f
    }

    fun draw(canvas: Canvas) {
        val scaledX = overlay.translateX(x)
        val scaledY = overlay.translateY(y)
        canvas.drawCircle(scaledX, scaledY, size, paint)
    }
}
