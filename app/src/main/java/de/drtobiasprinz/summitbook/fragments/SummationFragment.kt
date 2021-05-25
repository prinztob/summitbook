package de.drtobiasprinz.summitbook.fragments

import de.drtobiasprinz.summitbook.models.SummitEntry
import java.util.*

interface SummationFragment {
    fun update(filteredSummitEntries: ArrayList<SummitEntry>?)
}