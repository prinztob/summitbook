package de.drtobiasprinz.summitbook.models

import android.content.Context
import androidx.core.content.ContextCompat
import de.drtobiasprinz.summitbook.R
import de.drtobiasprinz.summitbook.db.entities.SportGroup
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import java.util.function.Supplier
import java.util.stream.Stream

enum class BarChartZAxisSelector(
    val nameId: Int,
    val getColors: (Context) -> List<Int>,
    val getStackLabels: (Context) -> Array<String>,
    val getValueForEntry: (Supplier<Stream<Summit?>?>, BarChartYAxisSelector, Int) -> FloatArray,
    val getStringIdForSelectedItem: (Int) -> Int
) {
    PerSportGroup(R.string.sport_group, { context ->
        SportGroup.entries.map {
            ContextCompat.getColor(
                context, it.color
            )
        }
    }, { context ->
        SportGroup.entries.map { context.getString(it.sportNameStringId) }.toTypedArray()
    }, { entriesSupplier, selectedYAxisSpinnerEntry, indoorHeightMeterPercent ->
        SportGroup.entries.map { sportGroup ->
            selectedYAxisSpinnerEntry.f(
                entriesSupplier.get()?.filter { it?.sportType in sportGroup.sportTypes },
                indoorHeightMeterPercent
            )
        }.toFloatArray()
    }, { stackIndex ->
        SportGroup.entries[stackIndex].sportNameStringId
    }),
    PerSportType(R.string.sport_type, { context ->
        SportType.entries.map {
            ContextCompat.getColor(
                context, it.color
            )
        }
    }, { context ->
        SportType.entries.map { context.getString(it.sportNameStringId) }.toTypedArray()
    }, { entriesSupplier, selectedYAxisSpinnerEntry, indoorHeightMeterPercent ->
        SportType.entries.map { sportType ->
            selectedYAxisSpinnerEntry.f(
                entriesSupplier.get()?.filter { it?.sportType == sportType },
                indoorHeightMeterPercent
            )
        }.toFloatArray()
    }, { stackIndex ->
        SportType.entries[stackIndex].sportNameStringId
    }),
    PerRoadType(R.string.road_type, { context ->
        RoadType.entries.map {
            it.color
        }
    }, { context ->
        RoadType.entries.map { context.getString(it.nameId) }.toTypedArray()
    }, { entriesSupplier, selectedYAxisSpinnerEntry, indoorHeightMeterPercent ->
        RoadType.entries.map { roadType ->
            entriesSupplier.get()?.toArray()?.sumOf { it ->
                ((it as Summit).distancePerRoadType[roadType]
                    ?: 0) * selectedYAxisSpinnerEntry.getValuePerRoadFactor(
                    it
                )
            } ?: 0.0
        }.map { it.toFloat() / 1000f }.toFloatArray()
    }, { stackIndex ->
        RoadType.entries[stackIndex].nameId
    }),
    PerSurface(R.string.road_surface, { context ->
        Surface.entries.map {
            it.color
        }
    }, { context ->
        Surface.entries.map { context.getString(it.nameId) }.toTypedArray()
    }, { entriesSupplier, selectedYAxisSpinnerEntry, indoorHeightMeterPercent ->
        Surface.entries.map { surface ->
            entriesSupplier.get()?.toArray()?.sumOf { it ->
                ((it as Summit).distancePerSurface[surface]
                    ?: 0) * selectedYAxisSpinnerEntry.getValuePerRoadFactor(
                    it
                )
            } ?: 0.0
        }.map { it.toFloat() / 1000f }.toFloatArray()
    }, { stackIndex ->
        Surface.entries[stackIndex].nameId
    }),
}