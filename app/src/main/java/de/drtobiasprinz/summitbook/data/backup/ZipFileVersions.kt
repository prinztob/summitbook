package de.drtobiasprinz.summitbook.data.backup

import de.drtobiasprinz.summitbook.data.db.entities.CyclingDynamicsData
import de.drtobiasprinz.summitbook.data.db.entities.ElevationData
import de.drtobiasprinz.summitbook.data.db.entities.GarminData
import de.drtobiasprinz.summitbook.data.db.entities.PowerData
import de.drtobiasprinz.summitbook.data.db.entities.SportType
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.db.entities.Summit.Companion.parseFromCsvFileLine
import de.drtobiasprinz.summitbook.data.db.entities.VelocityData
import de.drtobiasprinz.summitbook.data.backup.ZipFileVersionsUtils.Companion.getGarminDataV1AndV2
import de.drtobiasprinz.summitbook.data.backup.ZipFileVersionsUtils.Companion.getSummitDataV0AndV1
import de.drtobiasprinz.summitbook.data.backup.ZipFileVersionsUtils.Companion.getSummitDataV2

enum class ZipFileVersions(
    var versionName: String,
    var getSummit: (String) -> Summit,
    var getGarminData: (String) -> GarminData?
) {
    V0(
        "v0",
        { line -> getSummitDataV0AndV1(line) },
        { line ->
            val regex =
                """(?<activityId>(\d+));(?<garminIds>([\d,]+));(?<cal>([\d.]+));(?<averageHR>([\d.]+));(?<maxHR>([\d.]+));(?<power>([\d,.]+));(?<ftp>(\d+));(?<vo2max>([\d.]+));(?<aerobicTrainingEffect>([\d.]+));(?<anaerobicTrainingEffect>([\d.]+));(?<grit>([\d.]+));(?<flow>([\d.]+));(?<trainingLoad>([\d.]+))""".toRegex()
            val matchResult = regex.find(line.replace("\n", ""))
            if (matchResult != null) {
                GarminData(
                    matchResult.groups["garminIds"]!!.value.split(",") as MutableList<String>,
                    matchResult.groups["cal"]!!.value.toFloat(),
                    matchResult.groups["averageHR"]!!.value.toFloat(),
                    matchResult.groups["maxHR"]!!.value.toFloat(),
                    PowerData.parse(matchResult.groups["power"]!!.value.split(",")),
                    matchResult.groups["ftp"]!!.value.toInt(),
                    matchResult.groups["vo2max"]!!.value.toFloat(),
                    matchResult.groups["aerobicTrainingEffect"]!!.value.toFloat(),
                    matchResult.groups["anaerobicTrainingEffect"]!!.value.toFloat(),
                    matchResult.groups["grit"]!!.value.toFloat(),
                    matchResult.groups["flow"]!!.value.toFloat(),
                    matchResult.groups["trainingLoad"]!!.value.toFloat(),
                )
            } else {
                null
            }
        }),
    V1(
        "v1",
        { line -> getSummitDataV0AndV1(line) },
        { line -> getGarminDataV1AndV2(line) }
    ),
    V2(
        "v2",
        { line -> getSummitDataV2(line) },
        { line -> getGarminDataV1AndV2(line) }
    )
}

class ZipFileVersionsUtils {
    companion object {
        private const val LIST_PATTERN_ONCE = "[^;]+"
        private const val LIST_PATTERN_NULL_OR_ONCE = "[^;]*"
        fun getSummitDataV0AndV1(line: String): Summit {
            val regex =
                """(?<date>(\d{4}-\d{2}-\d{2}));(?<name>($LIST_PATTERN_ONCE));(?<sportType>(\w*));(?<activityId>(\d+));(?<kilometers>([\d.]+));(?<duration>([\d.]*));(?<elevationGain>(-?[\d.]+));(?<maxElevation>(-?[\d.]+));(?<maxVelocity>(-?[\d.]+));(?<lat>(-?[\d.]*));(?<long>(-?[\d.]*));(?<isFavorite>([01]));(?<isPeak>([01]));(?<comments>(.*));(?<participants>($LIST_PATTERN_NULL_OR_ONCE));(?<equipments>($LIST_PATTERN_NULL_OR_ONCE));(?<places>($LIST_PATTERN_NULL_OR_ONCE));(?<countries>($LIST_PATTERN_NULL_OR_ONCE))""".toRegex()
            val matchResult = regex.find(line.replace("\n", ""))
            if (matchResult != null) {
                val places = if (matchResult.groups["places"]!!.value != "") matchResult.groups["places"]!!.value.split(
                    ","
                ) else emptyList()
                val (cleanPlaces, connectedActivityIds) = Summit.splitConnectedActivityTokens(places)
                return Summit(
                    Summit.parseDate(matchResult.groups["date"]!!.value),
                    matchResult.groups["name"]!!.value,
                    try {
                        SportType.valueOf(matchResult.groups["sportType"]!!.value)
                    } catch (_: IllegalArgumentException) {
                        SportType.Other
                    },
                    cleanPlaces,
                    if (matchResult.groups["countries"]!!.value != "") matchResult.groups["countries"]!!.value.split(
                        ","
                    ) else emptyList(),
                    matchResult.groups["comments"]!!.value,
                    ElevationData(
                        maxElevation = matchResult.groups["maxElevation"]!!.value.toInt(),
                        elevationGain = matchResult.groups["elevationGain"]!!.value.toInt()
                    ),
                    matchResult.groups["kilometers"]!!.value.toDouble(),
                    VelocityData(matchResult.groups["maxVelocity"]!!.value.toDouble()),
                    if (matchResult.groups["lat"]!!.value != "") matchResult.groups["lat"]!!.value.toDouble() else null,
                    if (matchResult.groups["long"]!!.value != "") matchResult.groups["long"]!!.value.toDouble() else null,
                    if (matchResult.groups["participants"]!!.value != "") matchResult.groups["participants"]!!.value.split(
                        ","
                    ) else emptyList(),
                    if (matchResult.groups["equipments"]!!.value != "") matchResult.groups["equipments"]!!.value.split(
                        ","
                    ) else emptyList(),
                    matchResult.groups["isFavorite"]!!.value == "1",
                    matchResult.groups["isPeak"]!!.value == "1",
                    activityId = if (matchResult.groups["activityId"]!!.value != "") matchResult.groups["activityId"]!!.value.toLong() else System.currentTimeMillis(),
                    duration = matchResult.groups["duration"]!!.value.toInt(),
                    connectedActivityIds = connectedActivityIds
                )
            } else {
                return parseFromCsvFileLine(line)
            }
        }

        /**
         * v2 format: identical to v0/v1 plus a trailing `connectedActivityIds`
         * field after countries (see [Summit.getStringRepresentation]).
         * Parsed by splitting on `;` (the writer sanitizes semicolons out of
         * all fields), because the greedy comments group of the v0/v1 regex
         * mis-splits lines that carry trailing fields.
         */
        fun getSummitDataV2(line: String): Summit {
            val fields = line.replace("\n", "").split(";")
            if (fields.size < 18) {
                return parseFromCsvFileLine(line)
            }
            val (cleanPlaces, connectedFromPlaces) = Summit.splitConnectedActivityTokens(
                if (fields[16].isNotEmpty()) fields[16].split(",") else emptyList()
            )
            val connectedFromField = fields.getOrNull(18)?.takeIf { it.isNotEmpty() }
                ?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
            return Summit(
                Summit.parseDate(fields[0]),
                fields[1],
                try {
                    SportType.valueOf(fields[2])
                } catch (_: IllegalArgumentException) {
                    SportType.Other
                },
                cleanPlaces,
                if (fields[17].isNotEmpty()) fields[17].split(",") else emptyList(),
                fields[13],
                ElevationData(
                    maxElevation = fields[7].ifEmpty { "0" }.toInt(),
                    elevationGain = fields[6].ifEmpty { "0" }.toInt()
                ),
                fields[4].ifEmpty { "0" }.toDouble(),
                VelocityData(fields[8].ifEmpty { "0.0" }.toDouble()),
                if (fields[9].isNotEmpty()) fields[9].toDouble() else null,
                if (fields[10].isNotEmpty()) fields[10].toDouble() else null,
                if (fields[14].isNotEmpty()) fields[14].split(",") else emptyList(),
                if (fields[15].isNotEmpty()) fields[15].split(",") else emptyList(),
                fields[11] == "1",
                fields[12] == "1",
                activityId = if (fields[3].isNotEmpty()) fields[3].toLong() else System.currentTimeMillis(),
                duration = fields[5].ifEmpty { "0" }.toInt(),
                connectedActivityIds = (connectedFromPlaces + connectedFromField).distinct()
            )
        }

        fun getGarminDataV1AndV2(line: String): GarminData? {
            val regex =
                """(?<activityId>(\d+));(?<garminIds>([\d,]+));(?<cal>([\d.]+));(?<averageHR>([\d.]+));(?<maxHR>([\d.]+));(?<power>([\d,.]+));(?<ftp>(\d+));(?<vo2max>([\d.]+));(?<aerobicTrainingEffect>([\d.]+));(?<anaerobicTrainingEffect>([\d.]+));(?<grit>([\d.]+));(?<flow>([\d.]+));(?<trainingLoad>([\d.]+));(?<waterEstimated>([\d.]+));(?<gainSolarActivityTime>([\d.]+));(?<avgSolarChargePercent>([\d.]+));(?<surfaceTypeUnpavedPercentage>([\d.]+));(?<cyclingDynamic>([\d,.]+))""".toRegex()
            val matchResult = regex.find(line.replace("\n", ""))
            if (matchResult != null) {
                return GarminData(
                    matchResult.groups["garminIds"]!!.value.split(",") as MutableList<String>,
                    matchResult.groups["cal"]!!.value.toFloat(),
                    matchResult.groups["averageHR"]!!.value.toFloat(),
                    matchResult.groups["maxHR"]!!.value.toFloat(),
                    PowerData.parse(matchResult.groups["power"]!!.value.split(",")),
                    matchResult.groups["ftp"]!!.value.toInt(),
                    matchResult.groups["vo2max"]!!.value.toFloat(),
                    matchResult.groups["aerobicTrainingEffect"]!!.value.toFloat(),
                    matchResult.groups["anaerobicTrainingEffect"]!!.value.toFloat(),
                    matchResult.groups["grit"]!!.value.toFloat(),
                    matchResult.groups["flow"]!!.value.toFloat(),
                    matchResult.groups["trainingLoad"]!!.value.toFloat(),
                    matchResult.groups["waterEstimated"]!!.value.toInt(),
                    matchResult.groups["gainSolarActivityTime"]!!.value.toInt(),
                    matchResult.groups["avgSolarChargePercent"]!!.value.toInt(),
                    matchResult.groups["surfaceTypeUnpavedPercentage"]!!.value.toFloat(),
                    CyclingDynamicsData.parse(
                        matchResult.groups["cyclingDynamic"]!!.value.split(",")
                    ),
                )
            } else {
                return null
            }
        }
    }
}