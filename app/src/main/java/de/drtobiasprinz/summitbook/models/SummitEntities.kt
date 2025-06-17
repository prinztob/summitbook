package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Summit
import java.util.Collections


class SummitEntities(
    var type: SummitEntityType,
    var name: String,
    var count: Int,
    var distance: Double,
    var heightMeters: Int
)


enum class SummitEntityType(
    var getRelevantValueFromSummit: (Summit) -> List<String>,
    var setRelevantValueFromSummit: (Summit, String, String) -> Unit
) {
    COUNTRIES(
        { summit -> summit.countries },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.countries, oldValue, newValue)
        }
    ),
    PARTICIPANTS(
        getRelevantValueFromSummit = { summit -> summit.participants },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.participants, oldValue, newValue)
        }
    ),
    EQUIPMENTS(getRelevantValueFromSummit = { summit -> summit.equipments },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.equipments, oldValue, newValue)
        }
    ),
    PLACES_VISITED(getRelevantValueFromSummit = { summit -> summit.places + summit.name },
        { summit, oldValue, newValue ->
            Collections.replaceAll(summit.places, oldValue, newValue)
            if (summit.name == oldValue) {
                summit.name = newValue
            }
        }
    ),

}

