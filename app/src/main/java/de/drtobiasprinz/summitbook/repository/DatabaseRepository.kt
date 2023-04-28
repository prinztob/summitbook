package de.drtobiasprinz.summitbook.repository

import de.drtobiasprinz.summitbook.db.dao.SegmentsDao
import de.drtobiasprinz.summitbook.db.dao.SummitsDao
import de.drtobiasprinz.summitbook.db.entities.Summit
import javax.inject.Inject

class DatabaseRepository @Inject constructor(private val summitsDao: SummitsDao, private val segmentsDao: SegmentsDao) {

    suspend fun saveSummit(entity: Summit) = summitsDao.saveSummit(entity)
    suspend fun updateSummit(entity: Summit) = summitsDao.updateSummit(entity)
    suspend fun deleteSummit(entity: Summit) = summitsDao.deleteSummit(entity)
    fun getDetailsSummit(id: Long) = summitsDao.getSummit(id)
    fun getAllSummits() = summitsDao.getAllSummits()
    fun getAllBookmarks() = summitsDao.getAllBookmarks()
    fun searchSummit(name: String) = summitsDao.searchSummit(name)

    fun getAllSegments() = segmentsDao.getAllSegmentsFlow()

}