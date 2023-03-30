package de.drtobiasprinz.summitbook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.utils.DataStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseViewModel @Inject constructor(private val repository: DatabaseRepository) : ViewModel() {

    private val _contactsList = MutableLiveData<DataStatus<List<Summit>>>()
    val contactsList : LiveData<DataStatus<List<Summit>>>
        get() = _contactsList

    private val _contactDetails = MutableLiveData<DataStatus<Summit>>()
    val contactDetails :LiveData<DataStatus<Summit>>
        get() = _contactDetails

    init {
        getAllContacts()
    }

    fun saveContact(isEdite: Boolean, entity: Summit) = viewModelScope.launch {
        if (isEdite) {
            repository.updateTask(entity)
        } else {
            repository.saveContact(entity)
        }
    }

    fun deleteContact(entity: Summit) = viewModelScope.launch {
        repository.deleteContact(entity)
    }

    fun deleteAllContacts() = viewModelScope.launch {
        repository.deleteAllContacts()
    }

    fun getAllContacts() = viewModelScope.launch {
        _contactsList.postValue(DataStatus.loading())
        repository.getAllContacts()
            .catch { _contactsList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _contactsList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getSortedListASC() = viewModelScope.launch {
        _contactsList.postValue(DataStatus.loading())
        repository.getSortedListASC()
            .catch { _contactsList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _contactsList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getSortedListDESC() = viewModelScope.launch {
        _contactsList.postValue(DataStatus.loading())
        repository.getSortedListDESC()
            .catch { _contactsList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _contactsList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getSearchContacts(name: String) = viewModelScope.launch {
        repository.searchContact(name).collect() {
            _contactsList.postValue(DataStatus.success(it, it.isEmpty()))
        }
    }

    fun getDetailsContact(id: Long) = viewModelScope.launch {
        repository.getDetailsContact(id).collect {
            _contactDetails.postValue(DataStatus.success(it, false))
        }
    }

}