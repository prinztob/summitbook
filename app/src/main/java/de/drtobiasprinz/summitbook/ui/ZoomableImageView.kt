package de.drtobiasprinz.summitbook.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.min

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    
    private var mode = NONE
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var minScale = 1f
    private var maxScale = 4f
    private var currentScale = 1f
    
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    init {
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
        
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event)
                if (oldDist > 10f) {
                    savedMatrix.set(matrix)
                    midPoint(mid, event)
                    mode = ZOOM
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    matrix.set(savedMatrix)
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                    checkAndSetTranslate()
                } else if (mode == ZOOM) {
                    val newDist = spacing(event)
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                        checkAndSetScale()
                    }
                }
            }
        }

        imageMatrix = matrix
        return true
    }

    private fun checkAndSetTranslate() {
        val values = FloatArray(9)
        matrix.getValues(values)
        
        val transX = values[Matrix.MTRANS_X]
        val transY = values[Matrix.MTRANS_Y]
        
        val imageWidth = drawable?.intrinsicWidth?.toFloat() ?: 0f
        val imageHeight = drawable?.intrinsicHeight?.toFloat() ?: 0f
        
        val scaledWidth = imageWidth * currentScale
        val scaledHeight = imageHeight * currentScale
        
        var deltaX = 0f
        var deltaY = 0f
        
        if (scaledWidth <= width) {
            deltaX = (width - scaledWidth) / 2 - transX
        } else {
            if (transX > 0) {
                deltaX = -transX
            } else if (transX + scaledWidth < width) {
                deltaX = width - (transX + scaledWidth)
            }
        }
        
        if (scaledHeight <= height) {
            deltaY = (height - scaledHeight) / 2 - transY
        } else {
            if (transY > 0) {
                deltaY = -transY
            } else if (transY + scaledHeight < height) {
                deltaY = height - (transY + scaledHeight)
            }
        }
        
        matrix.postTranslate(deltaX, deltaY)
    }

    private fun checkAndSetScale() {
        val values = FloatArray(9)
        matrix.getValues(values)
        
        val scaleX = values[Matrix.MSCALE_X]
        currentScale = scaleX
        
        if (currentScale < minScale) {
            val scale = minScale / currentScale
            matrix.postScale(scale, scale, mid.x, mid.y)
            currentScale = minScale
        } else if (currentScale > maxScale) {
            val scale = maxScale / currentScale
            matrix.postScale(scale, scale, mid.x, mid.y)
            currentScale = maxScale
        }
        
        checkAndSetTranslate()
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor
            
            if (newScale in minScale..maxScale) {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                currentScale = newScale
                checkAndSetTranslate()
                imageMatrix = matrix
            }
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (currentScale > minScale) {
                // Reset to min scale
                matrix.reset()
                currentScale = minScale
            } else {
                // Zoom to 2x
                val scale = min(2f, maxScale) / currentScale
                matrix.postScale(scale, scale, e.x, e.y)
                currentScale = min(2f, maxScale)
                checkAndSetTranslate()
            }
            imageMatrix = matrix
            return true
        }
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        post {
            resetZoom()
        }
    }

    private fun resetZoom() {
        matrix.reset()
        currentScale = 1f
        
        drawable?.let { d ->
            val imageWidth = d.intrinsicWidth.toFloat()
            val imageHeight = d.intrinsicHeight.toFloat()
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            
            if (imageWidth > 0 && imageHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                // Calculate scale to fit image in view
                val scaleX = viewWidth / imageWidth
                val scaleY = viewHeight / imageHeight
                val scale = minOf(scaleX, scaleY)
                
                // Calculate translation to center the image
                val scaledWidth = imageWidth * scale
                val scaledHeight = imageHeight * scale
                val dx = (viewWidth - scaledWidth) / 2f
                val dy = (viewHeight - scaledHeight) / 2f
                
                matrix.setScale(scale, scale)
                matrix.postTranslate(dx, dy)
                
                currentScale = scale
                minScale = scale
            }
        }
        
        imageMatrix = matrix
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}