package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding

enum class OrderByAscDescButtonGroup(
    val query: String, val bindingId: (FragmentSortAndFilterBinding) -> Int
) {
    Ascending("ASC", { e -> e.buttonAscending.id }),
    Descending("DESC", { e -> e.buttonDescending.id }),
}