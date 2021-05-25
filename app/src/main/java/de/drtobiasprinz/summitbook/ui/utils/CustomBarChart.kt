package de.drtobiasprinz.summitbook.ui.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.github.mikephil.charting.charts.BarChart


open class CustomBarChart : BarChart {
    private var lowerYAxisSafeZonePaint: Paint? = null
    private var upperYAxisSafeZonePaint: Paint? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun init() {
        super.init()
        lowerYAxisSafeZonePaint = Paint()
        lowerYAxisSafeZonePaint?.style = Paint.Style.FILL
        upperYAxisSafeZonePaint = Paint()
        upperYAxisSafeZonePaint?.style = Paint.Style.FILL
        mGridBackgroundPaint.color = Color.rgb(240, 240, 240)
    }

    override fun onDraw(canvas: Canvas) {
        val limitLines = mAxisLeft.limitLines
        if (limitLines == null || limitLines.size != 1) {
            super.onDraw(canvas)
        } else {
            val l1 = limitLines[0]
            drawRect(lowerYAxisSafeZonePaint, 0f, l1.limit, canvas)
            drawRect(upperYAxisSafeZonePaint,l1.limit, l1.limit*50, canvas)
            super.onDraw(canvas)
        }
    }

    private fun drawRect(paint: Paint?, lowerLimit: Float, upperLimit: Float, canvas: Canvas) {
        val pts = FloatArray(4)
        pts[1] = lowerLimit
        pts[3] = upperLimit
        mLeftAxisTransformer.pointValuesToPixel(pts)
        paint?.let { canvas.drawRect(mViewPortHandler.contentLeft(), pts[1], mViewPortHandler.contentRight(), pts[3], it) }
    }

    fun setSafeZoneColor(lowerColor: Int, upperColor: Int) {
        lowerYAxisSafeZonePaint?.color = lowerColor
        upperYAxisSafeZonePaint?.color = upperColor
    }
}