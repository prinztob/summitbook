package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class HasGpxTrackButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (hasTrack = 1)", { e -> e.buttonGpxYes.id }, { e -> e.hasTrack }),
    No(" AND (hasTrack = 0)", { e -> e.buttonGpxNo.id }, { e -> !e.hasTrack }),
    Indifferent("", { e -> e.buttonGpxAll.id }, { true })
}