package de.drtobiasprinz.summitbook.repository

import de.drtobiasprinz.summitbook.db.dao.SummitsDao
import de.drtobiasprinz.summitbook.db.entities.Summit
import javax.inject.Inject

class DatabaseRepository @Inject constructor(private val dao: SummitsDao) {

    suspend fun saveContact(entity : Summit)=dao.saveContact(entity)
    suspend fun updateTask(entity: Summit)= dao.updateContact(entity)
    suspend fun deleteContact(entity : Summit)=dao.deleteContact(entity)
    fun getDetailsContact(id:Long)= dao.getContact(id)
    fun getAllContacts()=dao.getAllContacts()
    fun deleteAllContacts()=dao.deleteAllContacts()
    fun getSortedListASC()=dao.sortedASC()
    fun getSortedListDESC()=dao.sortedDESC()
    fun searchContact(name: String) = dao.searchContact(name)


}