package de.drtobiasprinz.summitbook.fragments

import de.drtobiasprinz.summitbook.models.Summit
import java.util.*

interface SummationFragment {
    fun update(filteredSummitEntries: List<Summit>?)
}