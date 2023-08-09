package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Summit

class RangeSliderValues(
    var getValue: (Summit) -> Float,
    var totalMin: Float = 0f,
    var selectedMin: Float = 0f,
    var selectedMax: Float = 0f,
    var totalMax: Float = 0f
) {
    fun filter(summit: Summit): Boolean {
        return if (selectedMin in (totalMin + 0.001f)..(totalMax - 0.001f) || selectedMax in (totalMin + 0.001f)..(totalMax - 0.001f)) {
            getValue(summit) in selectedMin..selectedMax
        } else {
            true
        }
    }
}