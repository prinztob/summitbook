package de.drtobiasprinz.summitbook.data.repository

import de.drtobiasprinz.summitbook.core.Constants.CONNECTED_ACTIVITY_PREFIX
import de.drtobiasprinz.summitbook.data.db.dao.*
import de.drtobiasprinz.summitbook.data.db.entities.*
import de.drtobiasprinz.summitbook.data.db.entities.RoadType
import de.drtobiasprinz.summitbook.data.db.entities.Surface
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DatabaseRepository @Inject constructor(
    private val summitsDao: SummitsDao,
    private val segmentDao: SegmentDao,
    private val forecastDao: ForecastDao,
    private val ignoredActivityDao: IgnoredActivityDao,
    private val entityEventDao: EntityEventDao,
    private val peakDao: PeakDao,
    private val dailyActivitySummaryDao: DailyActivitySummaryDao,
) {

    suspend fun saveSummit(entity: Summit) = summitsDao.saveSummit(entity)
    suspend fun saveSummits(entities: List<Summit>) = summitsDao.insertAll(entities)
    suspend fun updateSummit(entity: Summit) = summitsDao.updateSummit(entity)
    suspend fun updateSummits(entities: List<Summit>) = summitsDao.updateSummits(entities)
    suspend fun updateIgnoreSimplifyingTrack(summitId: Long, ignoreSimplifyingTrack: Boolean) =
        summitsDao.updateIgnoreSimplifyingTrack(summitId, ignoreSimplifyingTrack)
    suspend fun updateDistanceData(summitId: Long, distancePerSurface: Map<Surface, Int>, distancePerRoadType: Map<RoadType, Int>) =
        summitsDao.updateDistanceData(summitId, distancePerSurface, distancePerRoadType)
    suspend fun deleteSummit(entity: Summit) = summitsDao.deleteSummit(entity)
    fun getDetailsSummit(id: Long) = summitsDao.getSummit(id)
    fun getAllSummits() = summitsDao.getAllSummits()
    fun getSummitsPaginated(limit: Int, offset: Int) = summitsDao.getSummitsPaginated(limit, offset)

    /**
     * Moves legacy `ac_id:XXXXX` entries from the places column into the
     * connectedActivityIds column. Idempotent: no-op once no prefixed places remain.
     */
    suspend fun migrateConnectedActivityPrefixesToColumn() {
        val summits = getAllSummits().first()
        val toUpdate = mutableListOf<Summit>()
        for (summit in summits) {
            if (summit.places.any { it.startsWith(CONNECTED_ACTIVITY_PREFIX) }) {
                val (cleanPlaces, connectedIds) = Summit.splitConnectedActivityTokens(summit.places)
                summit.places = cleanPlaces
                summit.connectedActivityIds = (summit.connectedActivityIds + connectedIds).distinct()
                toUpdate.add(summit)
            }
        }
        if (toUpdate.isNotEmpty()) {
            updateSummits(toUpdate)
        }
    }
    fun getAllSegments() = segmentDao.getAllSegments()
    suspend fun deleteSegmentEntry(entity: SegmentEntry) = segmentDao.deleteSegmentEntry(entity)
    suspend fun deleteSegment(entity: Segment) = segmentDao.deleteSegment(entity)
    suspend fun saveSegmentDetails(entity: SegmentDetails) = segmentDao.addSegmentDetails(entity)
    suspend fun updateSegmentDetails(entity: SegmentDetails) =
        segmentDao.updateSegmentDetails(entity)

    suspend fun saveSegmentEntry(entity: SegmentEntry) = segmentDao.addSegmentEntry(entity)
    suspend fun updateSegmentEntry(entity: SegmentEntry) = segmentDao.updateSegmentEntry(entity)

    fun getAllMountainPasses() = segmentDao.getAllMountainPasses()
    fun getAllForecasts() = forecastDao.getAllForecasts()
    suspend fun saveForecast(entity: Forecast) = forecastDao.addForecast(entity)
    suspend fun updateForecast(entity: Forecast) = forecastDao.updateForecast(entity)

    fun getIgnoredActivities() = ignoredActivityDao.getAllIgnoredActivities()
    suspend fun saveIgnoredActivity(entity: IgnoredActivity) = ignoredActivityDao.add(entity)

    fun getPeaks() = peakDao.getAllPeaks()
    suspend fun savePeak(peak: Peak) = peakDao.add(peak)
    suspend fun deletePeak(peak: Peak) = peakDao.delete(peak)
    suspend fun updatePeak(peak: Peak) = peakDao.update(peak)
    suspend fun getPeakByName(name: String) = peakDao.getPeakByName(name)
    suspend fun getDuplicatePeakNames() = peakDao.getDuplicatePeakNames()
    suspend fun getPeaksByName(name: String) = peakDao.getPeaksByName(name)
    suspend fun deleteDuplicatePeaks() {
        val duplicateNames = getDuplicatePeakNames()
        duplicateNames.forEach { name ->
            val peaks = getPeaksByName(name)
            // Keep the first one (lowest id), delete the rest
            peaks.drop(1).forEach { peak ->
                deletePeak(peak)
            }
        }
    }

    suspend fun saveEntityEvent(entity: EntityEvent) = entityEventDao.add(entity)
    suspend fun updateEntityEvent(entity: EntityEvent) = entityEventDao.update(entity)
    suspend fun deleteEntityEvent(entity: EntityEvent) = entityEventDao.delete(entity)
    fun getEntityEvents() = entityEventDao.getAllEntityEvents()

    fun getAllDailyActivitySummary() = dailyActivitySummaryDao.getAllDailyActivitySummary()
    suspend fun saveDailyActivitySummary(entity: DailyActivitySummary) = dailyActivitySummaryDao.add(entity)
    suspend fun getDailyActivitySummaryByDateSync(activityId: Long) = dailyActivitySummaryDao.getDailyActivitySummaryByDateSync(activityId)

}
