package de.drtobiasprinz.summitbook.utils

import de.drtobiasprinz.summitbook.db.entities.CyclingDynamicsData
import de.drtobiasprinz.summitbook.db.entities.ElevationData
import de.drtobiasprinz.summitbook.db.entities.GarminData
import de.drtobiasprinz.summitbook.db.entities.PowerData
import de.drtobiasprinz.summitbook.db.entities.SportType
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.db.entities.Summit.Companion.parseFromCsvFileLine
import de.drtobiasprinz.summitbook.db.entities.VelocityData
import de.drtobiasprinz.summitbook.utils.ZipFileVersionsUtils.Companion.getGarminDataV1AndV2
import de.drtobiasprinz.summitbook.utils.ZipFileVersionsUtils.Companion.getSummitDataV0AndV1

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
                return Summit(
                    Summit.parseDate(matchResult.groups["date"]!!.value),
                    matchResult.groups["name"]!!.value,
                    try {
                        SportType.valueOf(matchResult.groups["sportType"]!!.value)
                    } catch (_: IllegalArgumentException) {
                        SportType.Other
                    },
                    if (matchResult.groups["places"]!!.value != "") matchResult.groups["places"]!!.value.split(
                        ","
                    ) else emptyList(),
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
                    duration = matchResult.groups["duration"]!!.value.toInt()
                )
            } else {
                return parseFromCsvFileLine(line)
            }
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