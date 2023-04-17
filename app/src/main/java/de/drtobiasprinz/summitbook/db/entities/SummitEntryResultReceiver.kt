package de.drtobiasprinz.summitbook.db.entities

import androidx.lifecycle.LiveData
import androidx.viewpager2.widget.ViewPager2
import de.drtobiasprinz.summitbook.utils.DataStatus

interface SummitEntryResultReceiver {

    fun getSummit(): Summit

    fun getSelectedSummitForComparison(): Summit?

    fun setSelectedSummitForComparison(summit: Summit?)

    fun getAllSummits(): LiveData<DataStatus<List<Summit>>>

    fun getViewPager(): ViewPager2

}