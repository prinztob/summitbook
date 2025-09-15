package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.util.Collections


class SummitEntitySummary(
    var type: SummitEntityType,
    var name: String,
    var count: Int,
    var distance: Double,
    var heightMeters: Int
)


enum class SummitEntityType(
    var getRelevantValueFromSummit: (Summit) -> List<String>,
    var setRelevantValueFromSummit: (Summit, String, String) -> Unit,
    var drawableIdDefault: Int,
    var drawableIdActive: Int? = null,
) {
    COUNTRIES(
        { summit -> summit.countries },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.countries, oldValue, newValue)
        },
        R.drawable.ic_baseline_flag_24
    ),
    PARTICIPANTS(
        getRelevantValueFromSummit = { summit -> summit.participants },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.participants, oldValue, newValue)
        },
        R.drawable.ic_baseline_people_24
    ),
    EQUIPMENTS(
        getRelevantValueFromSummit = { summit -> summit.equipments },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.equipments, oldValue, newValue)
        },
        R.drawable.ic_baseline_handyman_24
    ),
    PLACES_VISITED(
        getRelevantValueFromSummit = { summit -> summit.places + summit.name },
        { summit, oldValue, newValue ->
            val places = mutableListOf<String>()
            if (oldValue in summit.places) {
                summit.places.forEach {
                    if (it == oldValue) {
                        places.add(newValue)
                    } else {
                        places.add(it)
                    }
                }
                summit.places = places
            }
            if (summit.name == oldValue) {
                summit.name = newValue
            }
        },
        R.drawable.outline_landscape_2_off_24,
        R.drawable.outline_landscape_2_24,
    ),

}

