package de.drtobiasprinz.summitbook.repository

import androidx.sqlite.db.SupportSQLiteQuery
import de.drtobiasprinz.summitbook.db.dao.SummitsDao
import de.drtobiasprinz.summitbook.db.entities.Summit
import javax.inject.Inject

class DatabaseRepository @Inject constructor(private val dao: SummitsDao) {

    suspend fun saveContact(entity: Summit) = dao.saveContact(entity)
    suspend fun updateTask(entity: Summit) = dao.updateContact(entity)
    suspend fun deleteContact(entity: Summit) = dao.deleteContact(entity)
    fun getDetailsContact(id: Long) = dao.getContact(id)
    fun getAllContacts() = dao.getAllContacts()
    fun getSortedAndFilteredSummits(query: SupportSQLiteQuery) =
        dao.getSortedAndFilteredSummits(query)

    fun searchContact(name: String) = dao.searchContact(name)


}