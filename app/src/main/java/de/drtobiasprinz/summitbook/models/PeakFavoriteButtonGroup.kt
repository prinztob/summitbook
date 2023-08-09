package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class PeakFavoriteButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    IsFavorite(" AND (isFavorite = 1)", { e -> e.buttonMarkedFavorite.id }, { e -> e.isFavorite }),
    IsPeak(" AND (isPeak = 1)", { e -> e.buttonMarkedSummit.id }, { e -> e.isPeak }),
    Indifferent("", { e -> e.buttonMarkedAll.id }, { true })
}