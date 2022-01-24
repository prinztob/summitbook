package de.drtobiasprinz.summitbook.ui.utils

import android.graphics.Canvas
import android.graphics.Path
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

class MyLineLegendRenderer internal constructor(chart: LineDataProvider?, animator: ChartAnimator?, viewPortHandler: ViewPortHandler?) : LineChartRenderer(chart, animator, viewPortHandler) {
    // This method is same as its parent implementation. (Required so our version of generateFilledPath() is called.)
    override fun drawLinearFill(c: Canvas, dataSet: ILineDataSet, trans: Transformer, bounds: XBounds) {
        val filled = mGenerateFilledPathBuffer
        val startingIndex = bounds.min
        val endingIndex = bounds.range + bounds.min
        val indexInterval = 128
        var currentStartIndex: Int
        var currentEndIndex: Int
        var iterations = 0

        // Doing this iteratively in order to avoid OutOfMemory errors that can happen on large bounds sets.
        do {
            currentStartIndex = startingIndex + iterations * indexInterval
            currentEndIndex = currentStartIndex + indexInterval
            currentEndIndex = if (currentEndIndex > endingIndex) endingIndex else currentEndIndex
            if (currentStartIndex <= currentEndIndex) {
                generateFilledPath(dataSet, currentStartIndex, currentEndIndex, filled)
                trans.pathValueToPixel(filled)
                val drawable = dataSet.fillDrawable
                if (drawable != null) {
                    drawFilledPath(c, filled, drawable)
                } else {
                    drawFilledPath(c, filled, dataSet.fillColor, dataSet.fillAlpha)
                }
            }
            iterations++
        } while (currentStartIndex <= currentEndIndex)
    }

    // This method defines the perimeter of the area to be filled for horizontal bezier data sets.
    override fun drawCubicFill(c: Canvas, dataSet: ILineDataSet, spline: Path, trans: Transformer, bounds: XBounds) {
        val phaseY = mAnimator.phaseY

        //Call the custom method to retrieve the dataset for other line
        val boundaryEntries = (dataSet.fillFormatter as MyFillFormatter).fillLineBoundary

        // We are currently at top-last point, so draw down to the last boundary point
        val boundaryEntry = boundaryEntries[bounds.min + bounds.range]
        spline.lineTo(boundaryEntry.x, boundaryEntry.y * phaseY)

        // Draw a cubic line going back through all the previous boundary points
        var prev = dataSet.getEntryForIndex(bounds.min + bounds.range)
        var cur = prev
        for (x in bounds.min + bounds.range downTo bounds.min) {
            prev = cur
            cur = boundaryEntries[x]
            val cpx = prev.x + (cur.x - prev.x) / 2.0f
            spline.cubicTo(
                    cpx, prev.y * phaseY,
                    cpx, cur.y * phaseY,
                    cur.x, cur.y * phaseY)
        }

        // Join up the perimeter
        spline.close()
        trans.pathValueToPixel(spline)
        val drawable = dataSet.fillDrawable
        if (drawable != null) {
            drawFilledPath(c, spline, drawable)
        } else {
            drawFilledPath(c, spline, dataSet.fillColor, dataSet.fillAlpha)
        }
    }

    // This method defines the perimeter of the area to be filled for straight-line (default) data sets.
    private fun generateFilledPath(dataSet: ILineDataSet, startIndex: Int, endIndex: Int, outputPath: Path) {
        val phaseY = mAnimator.phaseY
        outputPath.reset()

        //Call the custom method to retrieve the dataset for other line
        val boundaryEntries = (dataSet.fillFormatter as MyFillFormatter).fillLineBoundary
        val entry = dataSet.getEntryForIndex(startIndex)
        val boundaryEntry = boundaryEntries[startIndex]

        // Move down to boundary of first entry
        outputPath.moveTo(entry.x, boundaryEntry.y * phaseY)

        // Draw line up to value of first entry
        outputPath.lineTo(entry.x, entry.y * phaseY)

        // Draw line across to the values of the next entries
        var currentEntry: Entry
        for (x in startIndex + 1..endIndex) {
            currentEntry = dataSet.getEntryForIndex(x)
            outputPath.lineTo(currentEntry.x, currentEntry.y * phaseY)
        }

        // Draw down to the boundary value of the last entry, then back to the first boundary value
        var boundaryEntry1: Entry
        for (x in endIndex downTo startIndex + 1) {
            boundaryEntry1 = boundaryEntries[x]
            outputPath.lineTo(boundaryEntry1.x, boundaryEntry1.y * phaseY)
        }

        // Join up the perimeter
        outputPath.close()
    }
}