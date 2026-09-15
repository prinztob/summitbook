package de.drtobiasprinz.summitbook.ui.filters

import de.drtobiasprinz.summitbook.data.db.entities.Summit

enum class HasImageButtonGroup(
    val query: String,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (imageIds != '')", { e -> e.imageIds.isNotEmpty() }),
    No(" AND (imageIds = '')", { e -> e.imageIds.isEmpty() }),
    Indifferent("", { true })
}