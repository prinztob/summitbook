package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Summit

enum class HasPositionButtonGroup(
    val query: String,
    val filter: (Summit) -> Boolean
) {
    Yes(
        " AND (lat IS NOT NULL) AND (lng IS NOT NULL)",
        { e -> e.lat != null && e.lng != null }),
    No(
        " AND (lat IS NULL) AND (lng IS NULL)",
        { e -> e.lat == null && e.lng == null }),
    Indifferent("", { true })
}