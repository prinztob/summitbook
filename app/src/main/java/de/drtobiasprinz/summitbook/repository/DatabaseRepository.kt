package de.drtobiasprinz.summitbook.repository

import de.drtobiasprinz.summitbook.db.dao.*
import de.drtobiasprinz.summitbook.db.entities.*
import javax.inject.Inject

class DatabaseRepository @Inject constructor(
    private val summitsDao: SummitsDao,
    private val segmentsDao: SegmentsDao,
    private val forecastDao: ForecastDao,
    private val solarIntensityDao: SolarIntensityDao,
    private val ignoredActivityDao: IgnoredActivityDao
) {

    suspend fun saveSummit(entity: Summit) = summitsDao.saveSummit(entity)
    suspend fun updateSummit(entity: Summit) = summitsDao.updateSummit(entity)
    suspend fun deleteSummit(entity: Summit) = summitsDao.deleteSummit(entity)
    fun getDetailsSummit(id: Long) = summitsDao.getSummit(id)
    fun getAllSummits() = summitsDao.getAllSummits()
    fun getAllBookmarks() = summitsDao.getAllBookmarks()
    fun searchSummit(name: String) = summitsDao.searchSummit(name)

    fun getAllSegments() = segmentsDao.getAllSegments()
    suspend fun deleteSegmentEntry(entity: SegmentEntry) = segmentsDao.deleteSegmentEntry(entity)
    suspend fun deleteSegment(entity: Segment) = segmentsDao.deleteSegment(entity)
    suspend fun saveSegmentDetails(entity: SegmentDetails) = segmentsDao.addSegmentDetails(entity)
    suspend fun updateSegmentDetails(entity: SegmentDetails) =
        segmentsDao.updateSegmentDetails(entity)

    suspend fun saveSegmentEntry(entity: SegmentEntry) = segmentsDao.addSegmentEntry(entity)
    suspend fun updateSegmentEntry(entity: SegmentEntry) = segmentsDao.updateSegmentEntry(entity)


    fun getAllForecasts() = forecastDao.getAllForecasts()
    suspend fun saveForecast(entity: Forecast) = forecastDao.addForecast(entity)
    suspend fun updateForecast(entity: Forecast) = forecastDao.updateForecast(entity)


    fun getAllSolarIntensities() = solarIntensityDao.getAllSolarIntensities()
    suspend fun saveSolarIntensity(entity: SolarIntensity) = solarIntensityDao.add(entity)
    suspend fun updateSolarIntensity(entity: SolarIntensity) = solarIntensityDao.update(entity)

    fun getIgnoredActivities() = ignoredActivityDao.getAllIgnoredActivities()
    suspend fun saveIgnoredActivity(entity: IgnoredActivity) = ignoredActivityDao.add(entity)
}