package de.drtobiasprinz.summitbook.data.analytics

import android.util.Log
import de.drtobiasprinz.summitbook.data.db.entities.SportGroup
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.db.entities.TrackBoundingBox
import de.drtobiasprinz.summitbook.data.model.GpsTrack
import io.ticofab.androidgpxparser.parser.domain.TrackPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Calculates how similar two activities and their tracks are.
 *
 * The score is the percentage of both tracks that follows the same line
 * (points within [NEAR_COURSE_METERS] of the other polyline), so it directly
 * answers "how much of the course is the same". A score of at least
 * [SAME_COURSE_THRESHOLD] means the same course was ridden again, which
 * allows adopting name/places/countries from the earlier entry.
 */
class TrackSimilarity {

    companion object {

        const val SAME_COURSE_THRESHOLD = 95.0
        private const val TAG = "TrackSimilarity"
        private const val EARTH_RADIUS_METERS = 6371000.0
        private const val METERS_PER_DEGREE = Math.PI / 180.0 * EARTH_RADIUS_METERS

        /** Max distance from the other track for a point to count as "on the same course". */
        private const val NEAR_COURSE_METERS = 30.0

        /** Distance between the resampled points used for the comparison. */
        private const val RESAMPLING_STEP_METERS = 25.0

        /** Upper bound of resampled points per track, keeps the comparison cheap. */
        private const val MAX_RESAMPLED_POINTS = 500

        /** Relative difference in kilometers for which two activities can be the same course. */
        private const val RELATIVE_LENGTH_TOLERANCE = 0.15

        /** How many existing entries are compared geometrically at most (newest first). */
        private const val MAX_CANDIDATES = 50

        /**
         * Candidate cap for [rankSimilarSummits]: higher than [MAX_CANDIDATES]
         * so no same-course ride is missed when ranking all of them for the
         * Times tab comparison, where every same-course activity counts.
         */
        private const val MAX_RANKED_CANDIDATES = 200

        fun isSameCourse(score: Double): Boolean = score >= SAME_COURSE_THRESHOLD

        /**
         * Cheap pre-filter that only looks at database properties,
         * so no track file has to be parsed for rejected candidates.
         */
        fun isPotentialSameCourse(newEntry: Summit, existingEntry: Summit): Boolean {
            val newBoundingBox = newEntry.trackBoundingBox
            val existingBoundingBox = existingEntry.trackBoundingBox
            if (newBoundingBox == null || existingBoundingBox == null) {
                return false
            }
            if (!intersects(newBoundingBox, existingBoundingBox)) {
                return false
            }
            val kilometersNew = newEntry.kilometers
            val kilometersExisting = existingEntry.kilometers
            if (kilometersNew <= 0.0 || kilometersExisting <= 0.0) {
                return false
            }
            if (abs(kilometersNew - kilometersExisting) / max(
                    kilometersNew, kilometersExisting
                ) > RELATIVE_LENGTH_TOLERANCE
            ) {
                return false
            }
            if (getSportGroup(newEntry) != getSportGroup(existingEntry)) {
                return false
            }
            return newEntry.hasGpsTrack() && existingEntry.hasGpsTrack()
        }

        /**
         * Compares both tracks geometrically. The result is the percentage of
         * points of both tracks that lie within [NEAR_COURSE_METERS] of the
         * respective other track, so 100 means both tracks follow the same line
         * everywhere. Independent of direction, speed and pauses.
         */
        fun similarityScore(trackA: List<TrackPoint>, trackB: List<TrackPoint>): Double {
            val resampledA = resampleTrack(trackA)
            val resampledB = resampleTrack(trackB)
            if (resampledA.isEmpty() || resampledB.isEmpty()) {
                return 0.0
            }
            val coverageA = coverage(resampledA, resampledB)
            val coverageB = coverage(resampledB, resampledA)
            return 100.0 * min(coverageA, coverageB)
        }

        /**
         * Finds the existing entry with the most similar track, or null if
         * no candidate passes the pre-filter.
         */
        fun findBestMatch(newEntry: Summit, existingSummits: List<Summit>): Match? {
            if (!newEntry.hasGpsTrack()) {
                return null
            }
            if (newEntry.trackBoundingBox == null) {
                newEntry.setBoundingBoxFromTrack()
            }
            val candidates = existingSummits
                .filter { it !== newEntry && it.activityId != newEntry.activityId }
                .filter { isPotentialSameCourse(newEntry, it) }
                .sortedByDescending { it.date }
                .take(MAX_CANDIDATES)
            if (candidates.isEmpty()) {
                return null
            }
            val trackPointsNew = getTrackPoints(newEntry) ?: return null
            var bestMatch: Match? = null
            for (candidate in candidates) {
                val trackPointsCandidate = getTrackPoints(candidate) ?: continue
                val score = similarityScore(trackPointsNew, trackPointsCandidate)
                if (bestMatch == null || score > bestMatch.score) {
                    bestMatch = Match(candidate, score)
                }
            }
            return bestMatch
        }

        /**
         * Ranks the given summits by track similarity to the given summit.
         * Only candidates that pass the cheap pre-filter ([isPotentialSameCourse])
         * are scored geometrically (at most [MAX_CANDIDATES] newest ones), so
         * this is fast enough to run when a single activity is shown. The
         * result contains every scored candidate, sorted by score descending.
         */
        fun rankSimilarSummits(
            summit: Summit,
            candidates: List<Summit>
        ): List<Pair<Summit, Double>> {
            if (!summit.hasGpsTrack()) {
                Log.i(TAG, "Not ranking similar tracks for ${summit.getDateAsString()}_${summit.name}: no GPS track")
                return emptyList()
            }
            if (summit.trackBoundingBox == null) {
                summit.setBoundingBoxFromTrack()
            }
            val filteredCandidates = candidates
                .filter { it !== summit && it.activityId != summit.activityId }
                .filter { isPotentialSameCourse(summit, it) }
                .sortedByDescending { it.date }
                .take(MAX_RANKED_CANDIDATES)
            if (filteredCandidates.isEmpty()) {
                Log.i(
                    TAG,
                    "No candidate passed the pre-filter for ${summit.getDateAsString()}_${summit.name} " +
                            "(${candidates.size} candidates in total)"
                )
                return emptyList()
            }
            val trackPointsSummit = getTrackPoints(summit)
            if (trackPointsSummit == null) {
                Log.i(
                    TAG,
                    "Could not parse the track of ${summit.getDateAsString()}_${summit.name}, " +
                            "so no similar tracks could be ranked"
                )
                return emptyList()
            }
            val ranked = filteredCandidates
                .mapNotNull { candidate ->
                    val trackPointsCandidate = getTrackPoints(candidate)
                        ?: return@mapNotNull null
                    candidate to similarityScore(trackPointsSummit, trackPointsCandidate)
                }
                .sortedByDescending { it.second }
            val skippedCount = filteredCandidates.size - ranked.size
            Log.i(
                TAG,
                "Ranked ${ranked.size} of ${filteredCandidates.size} pre-filtered candidates " +
                        "(${candidates.size} in total) for ${summit.getDateAsString()}_${summit.name}" +
                        (if (skippedCount > 0) {
                            ", $skippedCount had no parsable track"
                        } else {
                            ""
                        }) + ": " +
                        ranked.take(3).joinToString {
                            "${it.first.getDateAsString()}_${it.first.name} (score %.1f)".format(it.second)
                        }
            )
            return ranked
        }

        /**
         * Adopts name, places and countries on new entries from an earlier
         * entry that was done on the same course (score at least
         * [SAME_COURSE_THRESHOLD]). Entries that already got info adopted are
         * used as candidates for the following ones, so several rides of the
         * same new course all end up with the same name.
         *
         * Returns one [Adoption] per new entry that got info adopted.
         */
        fun adoptInfoFromSimilarEntries(
            newEntries: List<Summit>,
            existingSummits: List<Summit>
        ): List<Adoption> {
            val candidates = existingSummits.toMutableList()
            val adoptions = mutableListOf<Adoption>()
            for (newEntry in newEntries) {
                val bestMatch = findBestMatch(newEntry, candidates) ?: continue
                if (isSameCourse(bestMatch.score)) {
                    applyCourseInfoFromMatch(newEntry, bestMatch)
                    candidates.add(newEntry)
                    adoptions.add(Adoption(newEntry, bestMatch.summit, bestMatch.score))
                }
            }
            return adoptions
        }

        fun applyCourseInfoFromMatch(newEntry: Summit, match: Match) {
            Log.i(
                TAG,
                "Same course as ${match.summit.getDateAsString()}_${match.summit.name} " +
                        "(score ${match.score}): adopting name/places/countries for " +
                        newEntry.getDateAsString() + "_" + newEntry.name
            )
            newEntry.name = match.summit.name
            newEntry.places = match.summit.places
            newEntry.countries = match.summit.countries
            newEntry.isPeak = match.summit.isPeak
        }

        private fun getTrackPoints(summit: Summit): List<TrackPoint>? {
            // Parse a throwaway copy of the track: only lat/lon/elevation are
            // needed, so no YAML extensions file is passed. The track is
            // never attached to the Summit — candidates are shared,
            // long-lived objects and keeping their parsed tracks around
            // would bloat the heap. parseTrack prefers the simplified file
            // (much smaller) and falls back to the full one if that does not
            // yield any points.
            val track = GpsTrack(
                summit.getGpsTrackPath(),
                summit.getGpsTrackPath(simplified = true)
            )
            track.parseTrack()
            val trackPoints = track.trackPoints.map { it.first }
            return if (trackPoints.size >= 2) {
                trackPoints
            } else {
                null
            }
        }

        private fun getSportGroup(summit: Summit): SportGroup? =
            SportGroup.entries.firstOrNull { summit.sportType in it.sportTypes }

        private fun intersects(a: TrackBoundingBox, b: TrackBoundingBox): Boolean =
            !(a.lonEast <= b.lonWest || b.lonEast <= a.lonWest ||
                    a.latNorth <= b.latSouth || b.latNorth <= a.latSouth)

        /** Resamples a track to points with a uniform distance along the track. */
        private fun resampleTrack(track: List<TrackPoint>): List<LatLon> {
            if (track.size < 2) {
                return emptyList()
            }
            val points = track.map { LatLon(it.latitude, it.longitude) }
            val cumulativeDistances = DoubleArray(points.size)
            for (i in 1 until points.size) {
                cumulativeDistances[i] =
                    cumulativeDistances[i - 1] + distanceMeters(points[i - 1], points[i])
            }
            val totalLength = cumulativeDistances.last()
            if (totalLength <= 0.0) {
                return emptyList()
            }
            val step = max(RESAMPLING_STEP_METERS, totalLength / MAX_RESAMPLED_POINTS)
            val resampled = ArrayList<LatLon>()
            resampled.add(points.first())
            var segmentIndex = 0
            var targetDistance = step
            while (targetDistance < totalLength && segmentIndex < points.size - 1) {
                while (segmentIndex < points.size - 2 &&
                    cumulativeDistances[segmentIndex + 1] < targetDistance
                ) {
                    segmentIndex++
                }
                val segmentLength =
                    cumulativeDistances[segmentIndex + 1] - cumulativeDistances[segmentIndex]
                val fraction = if (segmentLength > 0.0) {
                    (targetDistance - cumulativeDistances[segmentIndex]) / segmentLength
                } else {
                    0.0
                }
                val segmentStart = points[segmentIndex]
                val segmentEnd = points[segmentIndex + 1]
                resampled.add(
                    LatLon(
                        segmentStart.lat + (segmentEnd.lat - segmentStart.lat) * fraction,
                        segmentStart.lon + (segmentEnd.lon - segmentStart.lon) * fraction
                    )
                )
                targetDistance += step
            }
            resampled.add(points.last())
            return resampled
        }

        /** Fraction of [points] that lies within [NEAR_COURSE_METERS] of [polyline]. */
        private fun coverage(points: List<LatLon>, polyline: List<LatLon>): Double {
            if (points.isEmpty()) {
                return 0.0
            }
            var pointsNearTrack = 0
            for (point in points) {
                var minDistance = Double.MAX_VALUE
                for (i in 0 until polyline.size - 1) {
                    val distance = pointToSegmentDistanceMeters(
                        point, polyline[i], polyline[i + 1]
                    )
                    if (distance < minDistance) {
                        minDistance = distance
                        if (minDistance <= NEAR_COURSE_METERS) {
                            break
                        }
                    }
                }
                if (minDistance <= NEAR_COURSE_METERS) {
                    pointsNearTrack++
                }
            }
            return pointsNearTrack.toDouble() / points.size
        }

        private fun pointToSegmentDistanceMeters(p: LatLon, a: LatLon, b: LatLon): Double {
            val cosLat = cos(Math.toRadians((p.lat + a.lat + b.lat) / 3.0))
            val abX = (b.lon - a.lon) * cosLat
            val abY = b.lat - a.lat
            val apX = (p.lon - a.lon) * cosLat
            val apY = p.lat - a.lat
            val abSquared = abX * abX + abY * abY
            val t = if (abSquared == 0.0) {
                0.0
            } else {
                ((apX * abX + apY * abY) / abSquared).coerceIn(0.0, 1.0)
            }
            val dx = apX - t * abX
            val dy = apY - t * abY
            return METERS_PER_DEGREE * sqrt(dx * dx + dy * dy)
        }

        private fun distanceMeters(a: LatLon, b: LatLon): Double {
            val cosLat = cos(Math.toRadians((a.lat + b.lat) / 2.0))
            val dLat = b.lat - a.lat
            val dLon = (b.lon - a.lon) * cosLat
            return METERS_PER_DEGREE * sqrt(dLat * dLat + dLon * dLon)
        }

        private data class LatLon(val lat: Double, val lon: Double)
    }
}

data class Match(val summit: Summit, val score: Double)

data class Adoption(
    val newEntry: Summit,
    val matchedEntry: Summit,
    val score: Double
)
