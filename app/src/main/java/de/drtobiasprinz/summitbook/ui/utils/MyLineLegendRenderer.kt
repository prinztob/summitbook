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

    override fun drawLinearFill(c: Canvas, dataSet: ILineDataSet, trans: Transformer, bounds: XBounds) {
        val filled = mGenerateFilledPathBuffer
        val startingIndex = bounds.min
        val endingIndex = bounds.range + bounds.min
        val indexInterval = 128
        var currentStartIndex: Int
        var currentEndIndex: Int
        var iterations = 0

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

    override fun drawCubicFill(c: Canvas, dataSet: ILineDataSet, spline: Path, trans: Transformer, bounds: XBounds) {
        val phaseY = mAnimator.phaseY
        val boundaryEntries = (dataSet.fillFormatter as MyFillFormatter).fillLineBoundary
        val boundaryEntry = boundaryEntries[bounds.min + bounds.range]
        spline.lineTo(boundaryEntry.x, boundaryEntry.y * phaseY)

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

        spline.close()
        trans.pathValueToPixel(spline)
        val drawable = dataSet.fillDrawable
        if (drawable != null) {
            drawFilledPath(c, spline, drawable)
        } else {
            drawFilledPath(c, spline, dataSet.fillColor, dataSet.fillAlpha)
        }
    }

    private fun generateFilledPath(dataSet: ILineDataSet, startIndex: Int, endIndex: Int, outputPath: Path) {
        val phaseY = mAnimator.phaseY
        outputPath.reset()

        if (dataSet.fillFormatter is MyFillFormatter) {

            val boundaryEntries = (dataSet.fillFormatter as MyFillFormatter).fillLineBoundary
            val entry = dataSet.getEntryForIndex(startIndex)
            val boundaryEntry = boundaryEntries[startIndex]

            outputPath.moveTo(entry.x, boundaryEntry.y * phaseY)
            outputPath.lineTo(entry.x, entry.y * phaseY)

            var currentEntry: Entry
            for (x in startIndex + 1..endIndex) {
                currentEntry = dataSet.getEntryForIndex(x)
                outputPath.lineTo(currentEntry.x, currentEntry.y * phaseY)
            }

            var boundaryEntry1: Entry
            for (x in endIndex downTo startIndex + 1) {
                boundaryEntry1 = boundaryEntries[x]
                outputPath.lineTo(boundaryEntry1.x, boundaryEntry1.y * phaseY)
            }
            outputPath.close()
        }
    }
}