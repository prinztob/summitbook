package de.drtobiasprinz.summitbook.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Peak(
    var name: String,
    var elevation: Int = 0
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    fun getStringRepresentation(): String {
        return "${name.replace(";", ",")};$elevation\n"
    }

    fun alreadyExists(peaks: List<Peak>): Boolean {
        return peaks.any { it.name == this.name }
    }

    companion object {
        private const val NUMBER_OF_ELEMENTS = 2

        fun getCsvHeadline(): String {
            return "Name;Elevation\n"
        }

        private fun checkValidNumberOfElements(splitLine: List<String>) {
            if (splitLine.size != NUMBER_OF_ELEMENTS) {
                throw Exception(
                    "Line ${splitLine.joinToString { ";" }} has ${splitLine.size} number " +
                            "of elements. Expected are $NUMBER_OF_ELEMENTS"
                )
            }
        }

        fun parseFromCsvFileLine(
            line: String,
            peaks: MutableList<Peak>,
            savePeak: (Peak) -> Unit
        ): Boolean {
            val splitLine = line.split(";")
            checkValidNumberOfElements(splitLine)
            val peak = Peak(
                splitLine[0],
                splitLine[1].toIntOrNull() ?: 0
            )
            return if (peak.alreadyExists(peaks)) {
                false
            } else {
                savePeak(peak)
                peaks.add(peak)
                true
            }
        }
    }
}
