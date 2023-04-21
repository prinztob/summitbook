package de.drtobiasprinz.summitbook.repository

import de.drtobiasprinz.summitbook.db.dao.SegmentsDao
import de.drtobiasprinz.summitbook.db.dao.SummitsDao
import de.drtobiasprinz.summitbook.db.entities.Summit
import javax.inject.Inject

class DatabaseRepository @Inject constructor(private val summitsDao: SummitsDao, private val segmentsDao: SegmentsDao) {

    suspend fun saveContact(entity: Summit) = summitsDao.saveContact(entity)
    suspend fun updateTask(entity: Summit) = summitsDao.updateContact(entity)
    suspend fun deleteContact(entity: Summit) = summitsDao.deleteContact(entity)
    fun getDetailsContact(id: Long) = summitsDao.getContact(id)
    fun getAllSummits() = summitsDao.getAllSummits()
    fun getAllBookmarks() = summitsDao.getAllBookmarks()
    fun searchContact(name: String) = summitsDao.searchContact(name)

    fun getAllSegments() = segmentsDao.getAllSegmentsFlow()

}