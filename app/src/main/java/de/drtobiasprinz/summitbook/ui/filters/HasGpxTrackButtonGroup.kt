package de.drtobiasprinz.summitbook.ui.filters

import de.drtobiasprinz.summitbook.data.db.entities.Summit

enum class HasGpxTrackButtonGroup(
    val query: String,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (hasTrack = 1)", { e -> e.hasTrack }),
    No(" AND (hasTrack = 0)", { e -> !e.hasTrack }),
    Indifferent("", { true })
}