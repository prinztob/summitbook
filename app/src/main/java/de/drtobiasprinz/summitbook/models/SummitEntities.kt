package de.drtobiasprinz.summitbook.models

import de.drtobiasprinz.summitbook.db.entities.Summit


class SummitEntities(
    var type: SummitEntityType,
    var name: String,
    var count: Int,
    var distance: Double,
    var heightMeters: Int
)


enum class SummitEntityType(
    var getRelevantValueFromSummit: (Summit) -> List<String>
) {
    COUNTRIES(
        getRelevantValueFromSummit = { summit -> summit.countries }),
    PARTICIPANTS(
        getRelevantValueFromSummit = { summit -> summit.participants }),
    EQUIPMENTS(getRelevantValueFromSummit = { summit -> summit.equipments }),
    PLACES_VISITED(getRelevantValueFromSummit = { summit -> summit.places + summit.name }),

}

