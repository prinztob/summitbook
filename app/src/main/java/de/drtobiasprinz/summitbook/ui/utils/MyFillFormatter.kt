package de.drtobiasprinz.summitbook.ui.utils

import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet


class MyFillFormatter //Pass the dataset of other line in the Constructor
@JvmOverloads constructor(private val boundaryDataSet: ILineDataSet? = null) : IFillFormatter {
    override fun getFillLinePosition(dataSet: ILineDataSet, dataProvider: LineDataProvider): Float {
        return 0f
    }

    //Define a new method which is used in the LineChartRenderer
    val fillLineBoundary: List<Entry>
        get() = if (boundaryDataSet != null) {
            (boundaryDataSet as LineDataSet).values
        } else emptyList()

}