package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class HasPositionButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    Yes(
        " AND (lat IS NOT NULL) AND (lng IS NOT NULL)",
        { e -> e.buttonPositionYes.id },
        { e -> e.lat != null && e.lng != null }),
    No(
        " AND (lat IS NULL) AND (lng IS NULL)",
        { e -> e.buttonPositionNo.id },
        { e -> e.lat == null && e.lng == null }),
    Indifferent("", { e -> e.buttonPositionAll.id }, { true })
}