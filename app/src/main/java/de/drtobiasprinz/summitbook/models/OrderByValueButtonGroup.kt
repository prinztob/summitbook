package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.databinding.FragmentSortAndFilterBinding
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Summit

enum class OrderByValueButtonGroup(
    val query: String,
    val bindingId: (FragmentSortAndFilterBinding) -> Int,
    val sort: (List<Summit>, OrderByAscDescButtonGroup) -> List<Summit>,
    val sortSegments: (List<Segment>, OrderByAscDescButtonGroup) -> List<Segment>
) {
    Date("date", { e -> e.buttonByDate.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.date.time }
        } else {
            e.sortedBy { it.date.time }
        }
    }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { segment -> segment.segmentEntries.maxByOrNull { it.date }?.date }
        } else {
            e.sortedBy { segment -> segment.segmentEntries.maxByOrNull { it.date }?.date }
        }
    }),
    Name("name", { e -> e.buttonByName.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.name }
        } else {
            e.sortedBy { it.name }
        }
    }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.segmentDetails.getDisplayName() }
        } else {
            e.sortedBy { it.segmentDetails.getDisplayName() }
        }
    }),
    HeightMeter("elevationGain", { e -> e.buttonByHeightMeter.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.elevationData.elevationGain }
        } else {
            e.sortedBy { it.elevationData.elevationGain }
        }
    }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.segmentEntries.firstOrNull()?.heightMetersUp }
        } else {
            e.sortedBy { it.segmentEntries.firstOrNull()?.heightMetersUp }
        }
    }),
    Kilometer("kilometer", { e -> e.buttonByKilometers.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.kilometers }
        } else {
            e.sortedBy { it.kilometers }
        }
    }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.segmentEntries.firstOrNull()?.kilometers }
        } else {
            e.sortedBy { it.segmentEntries.firstOrNull()?.kilometers }
        }
    }),
    TopElevation("maxElevation", { e -> e.buttonByElevation.id }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.elevationData.maxElevation }
        } else {
            e.sortedBy { it.elevationData.maxElevation }
        }
    }, { e, order ->
        if (order == OrderByAscDescButtonGroup.Descending) {
            e.sortedByDescending { it.segmentEntries.firstOrNull()?.heightMetersUp }
        } else {
            e.sortedBy { it.segmentEntries.firstOrNull()?.heightMetersUp }
        }
    })
}