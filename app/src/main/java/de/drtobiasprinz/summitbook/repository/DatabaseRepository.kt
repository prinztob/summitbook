package de.drtobiasprinz.summitbook.repository

import de.drtobiasprinz.summitbook.db.dao.*
import de.drtobiasprinz.summitbook.db.entities.*
import de.drtobiasprinz.summitbook.models.RoadType
import de.drtobiasprinz.summitbook.models.Surface
import javax.inject.Inject

class DatabaseRepository @Inject constructor(
    private val summitsDao: SummitsDao,
    private val segmentsDao: SegmentsDao,
    private val forecastDao: ForecastDao,
    private val ignoredActivityDao: IgnoredActivityDao,
    private val entityEventDao: EntityEventDao,
    private val peakDao: PeakDao,
    private val dailyActivitySummaryDao: DailyActivitySummaryDao,
) {

    suspend fun saveSummit(entity: Summit) = summitsDao.saveSummit(entity)
    suspend fun saveSummits(entities: List<Summit>) = summitsDao.insertAll(entities)
    suspend fun deleteAll() = summitsDao.deleteAll()
    suspend fun updateSummit(entity: Summit) = summitsDao.updateSummit(entity)
    suspend fun updateIgnoreSimplifyingTrack(summitId: Long, ignoreSimplifyingTrack: Boolean) =
        summitsDao.updateIgnoreSimplifyingTrack(summitId, ignoreSimplifyingTrack)
    suspend fun updateDistanceData(summitId: Long, distancePerSurface: Map<Surface, Int>, distancePerRoadType: Map<RoadType, Int>) =
        summitsDao.updateDistanceData(summitId, distancePerSurface, distancePerRoadType)
    suspend fun deleteSummit(entity: Summit) = summitsDao.deleteSummit(entity)
    fun getDetailsSummit(id: Long) = summitsDao.getSummit(id)
    fun getAllSummits() = summitsDao.getAllSummits()
    fun getAllSummitsLiveData() = summitsDao.getAllSummitsLiveData()
    fun getAllBookmarks() = summitsDao.getAllBookmarks()

    fun getAllSegments() = segmentsDao.getAllSegments()
    suspend fun deleteSegmentEntry(entity: SegmentEntry) = segmentsDao.deleteSegmentEntry(entity)
    suspend fun deleteSegment(entity: Segment) = segmentsDao.deleteSegment(entity)
    suspend fun saveSegmentDetails(entity: SegmentDetails) = segmentsDao.addSegmentDetails(entity)
    suspend fun updateSegmentDetails(entity: SegmentDetails) =
        segmentsDao.updateSegmentDetails(entity)

    suspend fun saveSegmentEntry(entity: SegmentEntry) = segmentsDao.addSegmentEntry(entity)
    suspend fun updateSegmentEntry(entity: SegmentEntry) = segmentsDao.updateSegmentEntry(entity)

    fun getAllForecasts() = forecastDao.getAllForecasts()
    fun getAllForecastsLiveData() = forecastDao.getAllForecastsLiveData()
    suspend fun saveForecast(entity: Forecast) = forecastDao.addForecast(entity)
    suspend fun updateForecast(entity: Forecast) = forecastDao.updateForecast(entity)

    fun getIgnoredActivities() = ignoredActivityDao.getAllIgnoredActivities()
    suspend fun saveIgnoredActivity(entity: IgnoredActivity) = ignoredActivityDao.add(entity)

    fun getPeaks() = peakDao.getAllPeaks()
    suspend fun savePeak(peak: Peak) = peakDao.add(peak)
    suspend fun deletePeak(peak: Peak) = peakDao.delete(peak)

    suspend fun saveEntityEvent(entity: EntityEvent) = entityEventDao.add(entity)
    suspend fun updateEntityEvent(entity: EntityEvent) = entityEventDao.update(entity)
    suspend fun deleteEntityEvent(entity: EntityEvent) = entityEventDao.delete(entity)
    fun getEntityEvents() = entityEventDao.getAllEntityEvents()

    fun getAllDailyActivitySummary() = dailyActivitySummaryDao.getAllDailyActivitySummary()
    suspend fun saveDailyActivitySummary(entity: DailyActivitySummary) = dailyActivitySummaryDao.add(entity)
    suspend fun getDailyActivitySummaryByDateSync(activityId: Long) = dailyActivitySummaryDao.getDailyActivitySummaryByDateSync(activityId)

}