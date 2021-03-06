package de.drtobiasprinz.summitbook.models

import androidx.viewpager2.widget.ViewPager2

interface SummitEntryResultReceiver {

    fun getSummit(): Summit

    fun getSelectedSummitForComparison(): Summit?

    fun setSelectedSummitForComparison(summit: Summit?)

    fun getSummitsForComparison(): List<Summit>

    fun getViewPager(): ViewPager2

}