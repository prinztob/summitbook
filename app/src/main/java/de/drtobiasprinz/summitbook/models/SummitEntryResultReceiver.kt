package de.drtobiasprinz.summitbook.models

import androidx.lifecycle.LiveData
import androidx.viewpager2.widget.ViewPager2
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.utils.DataStatus

interface SummitEntryResultReceiver {

    fun getSummit(): Summit

    fun getSelectedSummitForComparison(): Summit?

    fun setSelectedSummitForComparison(summit: Summit?)

    fun getAllSummits(): LiveData<DataStatus<List<Summit>>>

    fun getAllSegments(): LiveData<DataStatus<List<Segment>>>

    fun getViewPager(): ViewPager2

}