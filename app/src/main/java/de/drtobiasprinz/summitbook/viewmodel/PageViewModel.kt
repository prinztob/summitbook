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
class PageViewModel @Inject constructor(private val repository: DatabaseRepository) : ViewModel() {

    private var _summitsList = MutableLiveData<DataStatus<List<Summit>>>()
    val summitsList: LiveData<DataStatus<List<Summit>>>
        get() = _summitsList

    private val _segmentsList = MutableLiveData<DataStatus<List<Segment>>>()
    val segmentsList: LiveData<DataStatus<List<Segment>>>
        get() = _segmentsList

    private val _summitToView = MutableLiveData<DataStatus<Summit>>()
    val summitToView: LiveData<DataStatus<Summit>>
        get() = _summitToView

    private val _summitToCompare = MutableLiveData<DataStatus<Summit?>>()
    val summitToCompare: LiveData<DataStatus<Summit?>>
        get() = _summitToCompare

    init {
        getAllSummits()
        getAllSegments()
        setSummitToCompareToNull()
    }

    private fun getAllSummits() = viewModelScope.launch {
        _summitsList.postValue(DataStatus.loading())
        repository.getAllSummits()
            .catch { _summitsList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _summitsList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getSummitToView(id: Long) = viewModelScope.launch {
        repository.getDetailsSummit(id).collect {
            _summitToView.postValue(DataStatus.success(it, false))
        }
    }

    fun getSummitToCompare(id: Long) = viewModelScope.launch {
        repository.getDetailsSummit(id).collect {
            _summitToCompare.postValue(DataStatus.success(it, false))
        }
    }

    fun setSummitToCompareToNull() {
        _summitToCompare.postValue(DataStatus.success(null, false))
    }

    private fun getAllSegments() = viewModelScope.launch {
        repository.getAllSegments().collect {
            _segmentsList.postValue(DataStatus.success(it, false))
        }
    }
}