package de.drtobiasprinz.summitbook.ui.filters

import de.drtobiasprinz.summitbook.data.db.entities.Summit

enum class PeakFavoriteButtonGroup(
    val query: String,
    val filter: (Summit) -> Boolean
) {
    IsFavorite(" AND (isFavorite = 1)", { e -> e.isFavorite }),
    IsPeak(" AND (isPeak = 1)", { e -> e.isPeak }),
    Indifferent("", { true })
}