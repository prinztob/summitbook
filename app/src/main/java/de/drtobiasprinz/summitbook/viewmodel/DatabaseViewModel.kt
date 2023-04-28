package de.drtobiasprinz.summitbook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.drtobiasprinz.summitbook.db.entities.Segment
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.utils.DataStatus
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseViewModel @Inject constructor(private val repository: DatabaseRepository) :
    ViewModel() {

    private var _summitsList = MutableLiveData<DataStatus<List<Summit>>>()
    val summitsList: LiveData<DataStatus<List<Summit>>>
        get() = _summitsList

    private val _bookmarksList = MutableLiveData<DataStatus<List<Summit>>>()
    val bookmarksList: LiveData<DataStatus<List<Summit>>>
        get() = _bookmarksList

    private val _segmentsList = MutableLiveData<DataStatus<List<Segment>>>()
    val segmentsList: LiveData<DataStatus<List<Segment>>>
        get() = _segmentsList

    private val _summitDetails = MutableLiveData<DataStatus<Summit>>()
    val summitDetails: LiveData<DataStatus<Summit>>
        get() = _summitDetails

    init {
        getAllSummits()
        getAllSegments()
    }

    fun refresh() {
        _summitsList.value = _summitsList.value
    }

    fun saveSummit(isEdite: Boolean, entity: Summit) = viewModelScope.launch {
        if (isEdite) {
            repository.updateSummit(entity)
        } else {
            repository.saveSummit(entity)
        }
    }

    fun deleteSummit(entity: Summit) = viewModelScope.launch {
        repository.deleteSummit(entity)
    }

    private fun getAllSummits() = viewModelScope.launch {
        _summitsList.postValue(DataStatus.loading())
        repository.getAllSummits()
            .catch { _summitsList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _summitsList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getAllBookmarks() = viewModelScope.launch {
        _bookmarksList.postValue(DataStatus.loading())
        repository.getAllBookmarks()
            .catch { _bookmarksList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _bookmarksList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getSearchSummit(name: String) = viewModelScope.launch {
        repository.searchSummit(name).collect {
            _summitsList.postValue(DataStatus.success(it, it.isEmpty()))
        }
    }

    fun getDetailsSummit(id: Long) = viewModelScope.launch {
        repository.getDetailsSummit(id).collect {
            _summitDetails.postValue(DataStatus.success(it, false))
        }
    }

    fun getAllSegments() = viewModelScope.launch {
        repository.getAllSegments().collect {
            _segmentsList.postValue(DataStatus.success(it, false))
        }
    }

}