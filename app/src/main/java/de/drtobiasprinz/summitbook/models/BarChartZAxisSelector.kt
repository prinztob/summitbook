package de.drtobiasprinz.summitbook.models

import android.content.Context
import androidx.core.content.ContextCompat
import de.drtobiasprinz.summitbook.R
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
    SportGroup(
        R.string.sport_group,
        { context ->
            de.drtobiasprinz.summitbook.db.entities.SportGroup.entries.map {
                ContextCompat.getColor(
                    context,
                    it.color
                )
            }
        },
        { context ->
            de.drtobiasprinz.summitbook.db.entities.SportGroup.entries.map { context.getString(it.sportNameStringId) }
                .toTypedArray()
        },
        { entriesSupplier, selectedYAxisSpinnerEntry, indoorHeightMeterPercent ->
            de.drtobiasprinz.summitbook.db.entities.SportGroup.entries.map { sportGroup ->
                selectedYAxisSpinnerEntry.f(
                    entriesSupplier.get()?.filter { it?.sportType in sportGroup.sportTypes },
                    indoorHeightMeterPercent
                )
            }.toFloatArray()
        },
        { stackIndex ->
            de.drtobiasprinz.summitbook.db.entities.SportGroup.entries[stackIndex].sportNameStringId
        }
    ),
    SportType(
        R.string.sport_type,
        { context ->
            de.drtobiasprinz.summitbook.db.entities.SportType.entries.map {
                ContextCompat.getColor(
                    context,
                    it.color
                )
            }
        },
        { context ->
            de.drtobiasprinz.summitbook.db.entities.SportType.entries.map { context.getString(it.sportNameStringId) }
                .toTypedArray()
        },
        { entriesSupplier, selectedYAxisSpinnerEntry, indoorHeightMeterPercent ->
            de.drtobiasprinz.summitbook.db.entities.SportType.entries.map { sportType ->
                selectedYAxisSpinnerEntry.f(
                    entriesSupplier.get()?.filter { it?.sportType == sportType },
                    indoorHeightMeterPercent
                )
            }.toFloatArray()
        },
        { stackIndex ->
            de.drtobiasprinz.summitbook.db.entities.SportType.entries[stackIndex].sportNameStringId
        }
    ),
}