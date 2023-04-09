package de.drtobiasprinz.summitbook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import de.drtobiasprinz.summitbook.db.entities.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.utils.DataStatus
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseViewModel @Inject constructor(private val repository: DatabaseRepository) :
    ViewModel() {

    private val _contactsList = MutableLiveData<DataStatus<List<Summit>>>()
    val contactsList: LiveData<DataStatus<List<Summit>>>
        get() = _contactsList

    private val _contactDetails = MutableLiveData<DataStatus<Summit>>()
    val contactDetails: LiveData<DataStatus<Summit>>
        get() = _contactDetails

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

    fun getSortedAndFilteredSummits(sortFilterValues: SortFilterValues) = viewModelScope.launch {
        _contactsList.postValue(DataStatus.loading())
        repository.getSortedAndFilteredSummits(SimpleSQLiteQuery(sortFilterValues.getSqlQuery()))
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