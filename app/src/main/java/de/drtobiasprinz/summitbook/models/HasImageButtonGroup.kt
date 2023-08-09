package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class HasImageButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val filter: (Summit) -> Boolean
) {
    Yes(" AND (imageIds != '')", { e -> e.buttonImageYes.id }, { e -> e.imageIds.isNotEmpty() }),
    No(" AND (imageIds = '')", { e -> e.buttonImageNo.id }, { e -> e.imageIds.isEmpty() }),
    Indifferent("", { e -> e.buttonImageAll.id }, { true })
}