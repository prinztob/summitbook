package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.ui.HackyViewPager

interface SummitEntryResultReceiver {

    fun getSummit(): Summit

    fun getSelectedSummitForComparison(): Summit?

    fun setSelectedSummitForComparison(summit: Summit?)

    fun getSummitsForComparison(): List<Summit>

    fun getViewPager(): HackyViewPager

}