package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Summit

enum class PeakFavoriteButtonGroup(
    val query: String,
    val filter: (Summit) -> Boolean
) {
    IsFavorite(" AND (isFavorite = 1)", { e -> e.isFavorite }),
    IsPeak(" AND (isPeak = 1)", { e -> e.isPeak }),
    Indifferent("", { true })
}