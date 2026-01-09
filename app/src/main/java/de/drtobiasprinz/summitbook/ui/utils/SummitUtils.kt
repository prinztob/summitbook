package de.drtobiasprinz.summitbook.ui.utils

import de.drtobiasprinz.summitbook.db.entities.SportGroup
import de.drtobiasprinz.summitbook.db.entities.Summit

class SummitUtils {

    companion object {

        fun getSummitsToCompare(
            summits: List<Summit>,
            summitEntry: Summit,
            onlyWithGpxTrack: Boolean = false,
            onlyWithPowerData: Boolean = false,
        ): List<Summit> {
            summits.let { summits ->
                val sportGroup =
                    SportGroup.entries
                        .filter { summitEntry.sportType in it.sportTypes }
                return if (sportGroup.size == 1) {
                    summits.filter {
                        it.id != summitEntry.id && it.hasGpsTrack() &&
                                it.sportType in sportGroup.first().sportTypes
                    }
                } else {
                    summits.filter {
                        it.id != summitEntry.id &&
                                (if (onlyWithGpxTrack) it.hasGpsTrack() else true) &&
                                (if (onlyWithPowerData) it.garminData?.power != null else true) &&
                                it.sportType == summitEntry.sportType
                    }
                }
            }
        }

    }
}