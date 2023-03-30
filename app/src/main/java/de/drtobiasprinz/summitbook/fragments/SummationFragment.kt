package de.drtobiasprinz.summitbook.fragments

import de.drtobiasprinz.summitbook.db.entities.Summit

interface SummationFragment {
    fun update(filteredSummitEntries: List<Summit>?)
}