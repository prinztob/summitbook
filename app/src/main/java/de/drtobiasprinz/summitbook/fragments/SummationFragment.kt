package de.drtobiasprinz.summitbook.fragments

import de.drtobiasprinz.summitbook.models.Summit

interface SummationFragment {
    fun update(filteredSummitEntries: List<Summit>?)
}