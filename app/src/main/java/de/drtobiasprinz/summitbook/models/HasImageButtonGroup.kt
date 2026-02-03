package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Summit

enum class HasImageButtonGroup(
    val query: String,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (imageIds != '')", { e -> e.imageIds.isNotEmpty() }),
    No(" AND (imageIds = '')", { e -> e.imageIds.isEmpty() }),
    Indifferent("", { true })
}