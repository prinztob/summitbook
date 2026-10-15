package de.drtobiasprinz.summitbook.data.model

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.drtobiasprinz.summitbook.core.utils.JsonUtils
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import kotlin.math.abs

/**
 * Fastest time over a rolling distance window, e.g. the fastest 5 km of a
 * ride, together with where on the track the effort happened.
 */
data class BestEffort(
    val timeSeconds: Double,
    val startDistanceMeters: Double,
    val endDistanceMeters: Double
)

/**
 * A sustained ascent or descent section of the track, with its average
 * power and heart rate when the track has that data.
 */
data class ElevationInterval(
    val startDistanceMeters: Double,
    val endDistanceMeters: Double,
    val startElevation: Double,
    val endElevation: Double,
    val durationSeconds: Double,
    val power: Double? = null,
    val hr: Double? = null
) {
    val lengthMeters: Double get() = endDistanceMeters - startDistanceMeters
    val elevationDelta: Double get() = endElevation - startElevation
    val midpointDistanceMeters: Double get() = (startDistanceMeters + endDistanceMeters) / 2.0
}

/**
 * Interval data of one activity, computed by the Python analysis pipeline
 * and stored in the per-activity `id_<activityId>_gpxpy.json` file.
 */
data class TrackIntervalData(
    val kmTimes: List<Double>,
    val bestEfforts: Map<Int, BestEffort>,
    val ascents: List<ElevationInterval>,
    val descents: List<ElevationInterval>,
    val bestPowerMin: Map<Int, Double> = emptyMap(),
    val bestPowerKm: Map<Int, Double> = emptyMap(),
    val bestHrMin: Map<Int, Double> = emptyMap(),
    val bestHrKm: Map<Int, Double> = emptyMap(),
    val bestDistanceMin: Map<Int, Double> = emptyMap(),
    val powerPerKm: List<Double> = emptyList(),
    val hrPerKm: List<Double> = emptyList()
) {
    companion object {

        /**
         * Schema version of the interval data the app understands. Bumped in
         * the Python analysis together with its `INTERVAL_DATA_VERSION`, so
         * files from older analyses are recomputed once.
         */
        private const val INTERVAL_DATA_VERSION = 2

        private const val KEY_KM_TIMES = "km_times"
        private const val KEY_BEST_EFFORTS = "best_efforts_km"
        private const val KEY_ASCENTS = "ascents"
        private const val KEY_DESCENTS = "descents"
        private const val KEY_BEST_POWER_MIN = "best_power_min"
        private const val KEY_BEST_POWER_KM = "best_power_km"
        private const val KEY_BEST_HR_MIN = "best_hr_min"
        private const val KEY_BEST_HR_KM = "best_hr_km"
        private const val KEY_BEST_DISTANCE_MIN = "best_distance_min"
        private const val KEY_POWER_PER_KM = "power_per_km"
        private const val KEY_HR_PER_KM = "hr_per_km"
        private const val KEY_INTERVAL_DATA_VERSION = "interval_data_version"

        fun fromJson(json: JsonObject): TrackIntervalData? {
            // All keys must be present and the schema version must match; a
            // partially written or pre-power/HR file is treated as missing
            // so the computation is retried
            if (!json.has(KEY_KM_TIMES) || !json.has(KEY_BEST_EFFORTS) ||
                !json.has(KEY_ASCENTS) || !json.has(KEY_DESCENTS) ||
                !json.has(KEY_BEST_POWER_MIN) || !json.has(KEY_BEST_POWER_KM) ||
                !json.has(KEY_BEST_HR_MIN) || !json.has(KEY_BEST_HR_KM) ||
                !json.has(KEY_BEST_DISTANCE_MIN) ||
                !json.has(KEY_POWER_PER_KM) || !json.has(KEY_HR_PER_KM) ||
                !json.has(KEY_INTERVAL_DATA_VERSION) ||
                json.get(KEY_INTERVAL_DATA_VERSION).asInt != INTERVAL_DATA_VERSION
            ) {
                return null
            }
            val kmTimes = json.getAsJsonArray(KEY_KM_TIMES).map { it.asDouble }
            val bestEfforts = mutableMapOf<Int, BestEffort>()
            json.getAsJsonObject(KEY_BEST_EFFORTS).keySet().forEach { key ->
                val effort = json.getAsJsonObject(KEY_BEST_EFFORTS).getAsJsonObject(key)
                bestEfforts[key.toInt()] = BestEffort(
                    effort.get("time").asDouble,
                    effort.get("start_distance").asDouble,
                    effort.get("end_distance").asDouble
                )
            }
            return TrackIntervalData(
                kmTimes = kmTimes,
                bestEfforts = bestEfforts,
                ascents = parseIntervals(json, KEY_ASCENTS),
                descents = parseIntervals(json, KEY_DESCENTS),
                bestPowerMin = parseWindowMap(json, KEY_BEST_POWER_MIN),
                bestPowerKm = parseWindowMap(json, KEY_BEST_POWER_KM),
                bestHrMin = parseWindowMap(json, KEY_BEST_HR_MIN),
                bestHrKm = parseWindowMap(json, KEY_BEST_HR_KM),
                bestDistanceMin = parseWindowMap(json, KEY_BEST_DISTANCE_MIN),
                powerPerKm = json.getAsJsonArray(KEY_POWER_PER_KM).map { it.asDouble },
                hrPerKm = json.getAsJsonArray(KEY_HR_PER_KM).map { it.asDouble }
            )
        }

        private fun parseWindowMap(json: JsonObject, key: String): Map<Int, Double> =
            json.getAsJsonObject(key).keySet().associateWith { window ->
                json.getAsJsonObject(key).getAsJsonPrimitive(window).asDouble
            }.mapKeys { it.key.toInt() }

        private fun parseIntervals(json: JsonObject, key: String): List<ElevationInterval> =
            json.getAsJsonArray(key).map { element ->
                val interval = element.asJsonObject
                ElevationInterval(
                    startDistanceMeters = interval.get("start_distance").asDouble,
                    endDistanceMeters = interval.get("end_distance").asDouble,
                    startElevation = interval.get("start_elevation").asDouble,
                    endElevation = interval.get("end_elevation").asDouble,
                    durationSeconds = interval.get("duration").asDouble,
                    power = parseNullableDouble(interval, "power"),
                    hr = parseNullableDouble(interval, "hr")
                )
            }

        private fun parseNullableDouble(json: JsonObject, key: String): Double? {
            if (!json.has(key) || json.get(key).isJsonNull) {
                return null
            }
            return json.get(key).asDouble
        }
    }
}

/**
 * Reads the interval data of [summit] from its gpxpy.json file. Returns null
 * when the file is missing or was written before the interval computation
 * existed, so callers can trigger the backfill.
 */
fun readTrackIntervalData(summit: Summit): TrackIntervalData? {
    val file = summit.getGpxPyPath().toFile()
    if (!file.exists()) {
        return null
    }
    return try {
        val jsonString = JsonUtils.getJsonData(file) ?: return null
        val json = JsonParser.parseString(jsonString) as? JsonObject ?: return null
        TrackIntervalData.fromJson(json)
    } catch (e: Exception) {
        null
    }
}

/**
 * Seconds per each consecutive [rangeKm]-km interval of the ride; a trailing
 * interval that does not span the full range is dropped.
 */
fun splitTimesForRange(kmTimes: List<Double>, rangeKm: Int): List<Double> =
    kmTimes.chunked(rangeKm).filter { it.size == rangeKm }.map { it.sum() }

/**
 * How far apart the midpoints of two ascents/descents may lie to count as the
 * same section of a course when comparing same-course activities.
 */
const val ELEVATION_INTERVAL_MATCH_TOLERANCE_M = 500.0

/**
 * Finds the interval in [others] that covers the same part of the course as
 * [interval], matched by midpoint distance. Returns null when no candidate
 * lies within the match tolerance.
 */
fun matchElevationInterval(
    interval: ElevationInterval,
    others: List<ElevationInterval>
): ElevationInterval? {
    var best: ElevationInterval? = null
    var bestDistance = Double.MAX_VALUE
    for (other in others) {
        val distance = abs(other.midpointDistanceMeters - interval.midpointDistanceMeters)
        if (distance < bestDistance) {
            bestDistance = distance
            best = other
        }
    }
    return if (best != null && bestDistance <= ELEVATION_INTERVAL_MATCH_TOLERANCE_M) {
        best
    } else {
        null
    }
}

/**
 * For each interval of the current ride's splits over [rangeKm], the fastest
 * time that any of the same-course [others] achieved in that same interval,
 * or null when no other ride covers it. Same-course rides run the same
 * track, so interval index i marks the same part of the course everywhere.
 */
fun fastestSplitTimes(
    currentKmTimes: List<Double>,
    rangeKm: Int,
    others: List<TrackIntervalData>
): List<Double?> {
    val othersSplits = others.map { splitTimesForRange(it.kmTimes, rangeKm) }
    return splitTimesForRange(currentKmTimes, rangeKm).indices.map { index ->
        othersSplits.mapNotNull { it.getOrNull(index) }.minOrNull()
    }
}

/**
 * The fastest duration any same-course ride achieved on the section of the
 * course covered by [interval], or null when no other ride has a section
 * matching it.
 */
fun fastestMatchedDuration(
    interval: ElevationInterval,
    others: List<ElevationInterval>
): Double? =
    others
        .filter {
            abs(it.midpointDistanceMeters - interval.midpointDistanceMeters) <=
                    ELEVATION_INTERVAL_MATCH_TOLERANCE_M
        }
        .minOfOrNull { it.durationSeconds }

/**
 * The best value any same-course ride achieved on the section of the
 * course covered by [interval], or null when no other ride has a section
 * matching it. The [selector] picks the value to compare (average power,
 * heart rate); higher is better.
 */
fun bestMatchedValue(
    interval: ElevationInterval,
    others: List<ElevationInterval>,
    selector: (ElevationInterval) -> Double?
): Double? =
    others
        .filter {
            abs(it.midpointDistanceMeters - interval.midpointDistanceMeters) <=
                    ELEVATION_INTERVAL_MATCH_TOLERANCE_M
        }
        .mapNotNull { selector(it) }
        .maxOrNull()

/**
 * Time-weighted average of the per-km values ([perKmValues], aligned with
 * the per-km times) over each consecutive [rangeKm]-km interval; trailing
 * intervals that do not span the full range are dropped.
 */
fun splitValuesForRange(
    perKmValues: List<Double>,
    kmTimes: List<Double>,
    rangeKm: Int
): List<Double> {
    if (perKmValues.isEmpty()) {
        return emptyList()
    }
    return perKmValues.indices
        .chunked(rangeKm)
        .filter { it.size == rangeKm }
        .map { chunk ->
            var weightedSum = 0.0
            var totalTime = 0.0
            chunk.forEach { index ->
                val seconds = kmTimes.getOrElse(index) { 0.0 }
                weightedSum += perKmValues[index] * seconds
                totalTime += seconds
            }
            if (totalTime > 0) weightedSum / totalTime else 0.0
        }
}

/**
 * For each interval of the current ride's power/HR splits, the best value
 * that any of the same-course [others] achieved in that same interval, or
 * null when no other ride covers it. The [selector] yields one value per
 * interval of the given range for each ride. Power, heart rate and covered
 * distance are "higher is better".
 */
fun bestSplitValues(
    others: List<TrackIntervalData>,
    selector: (TrackIntervalData) -> List<Double>
): List<Double?> {
    val othersSplits = others.map { selector(it) }
    val maxSize = othersSplits.maxOfOrNull { it.size } ?: 0
    return (0 until maxSize).map { index ->
        othersSplits.mapNotNull { it.getOrNull(index) }.maxOrNull()
    }
}

/**
 * The best value any same-course ride achieved over the given [window]
 * (minutes or km, depending on the map the [selector] returns), or null
 * when no other ride has a value for it. Higher is better: power, heart
 * rate and covered distance.
 */
fun bestWindowValue(
    others: List<TrackIntervalData>,
    window: Int,
    selector: (TrackIntervalData) -> Map<Int, Double>
): Double? =
    others.mapNotNull { selector(it)[window] }.maxOrNull()
