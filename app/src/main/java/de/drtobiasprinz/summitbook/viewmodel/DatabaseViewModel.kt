package de.drtobiasprinz.summitbook.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.drtobiasprinz.summitbook.db.entities.*
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

    private val _forecastsList = MutableLiveData<DataStatus<List<Forecast>>>()
    val forecastList: LiveData<DataStatus<List<Forecast>>>
        get() = _forecastsList

    private val _dailyReportDataList = MutableLiveData<DataStatus<List<DailyReportData>>>()
    val dailyReportDataList: LiveData<DataStatus<List<DailyReportData>>>
        get() = _dailyReportDataList

    private val _ignoredActivityList = MutableLiveData<DataStatus<List<IgnoredActivity>>>()
    val ignoredActivityList: LiveData<DataStatus<List<IgnoredActivity>>>
        get() = _ignoredActivityList

    private val _summitDetails = MutableLiveData<DataStatus<Summit>>()
    val summitDetails: LiveData<DataStatus<Summit>>
        get() = _summitDetails

    init {
        getAllSummits()
        getAllSegments()
        getAllForecasts()
        getAllDailyReportData()
        getAllIgnoredActivities()
    }

    fun refresh() {
        _summitsList.value = _summitsList.value
        _segmentsList.value = _segmentsList.value
    }

    fun saveSummit(isEdite: Boolean, entity: Summit) = viewModelScope.launch {
        if (isEdite) {
            repository.updateSummit(entity)
        } else {
            entity.id = repository.saveSummit(entity)
        }
    }

    fun deleteSummit(entity: Summit) = viewModelScope.launch {
        repository.deleteSummit(entity)
    }

    fun getAllSummits() = viewModelScope.launch {
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

    fun getDetailsSummit(id: Long) = viewModelScope.launch {
        repository.getDetailsSummit(id).collect {
            _summitDetails.postValue(DataStatus.success(it, false))
        }
    }

    private fun getAllSegments() = viewModelScope.launch {
        repository.getAllSegments().collect {
            _segmentsList.postValue(DataStatus.success(it, false))
        }
    }

    fun deleteSegmentEntry(entity: SegmentEntry) = viewModelScope.launch {
        repository.deleteSegmentEntry(entity)
    }

    fun deleteSegment(entity: Segment) = viewModelScope.launch {
        repository.deleteSegment(entity)
    }

    fun saveSegmentDetails(isEdite: Boolean, entity: SegmentDetails) = viewModelScope.launch {
        if (isEdite) {
            repository.updateSegmentDetails(entity)
        } else {
            entity.segmentDetailsId = repository.saveSegmentDetails(entity)
        }
    }

    fun saveSegmentEntry(isEdite: Boolean, entity: SegmentEntry) = viewModelScope.launch {
        if (isEdite) {
            repository.updateSegmentEntry(entity)
        } else {
            repository.saveSegmentEntry(entity)
        }
    }

    private fun getAllForecasts() = viewModelScope.launch {
        repository.getAllForecasts().collect {
            _forecastsList.postValue(DataStatus.success(it, false))
        }
    }

    fun saveForecast(isEdite: Boolean, entity: Forecast) = viewModelScope.launch {
        if (isEdite) {
            repository.updateForecast(entity)
        } else {
            repository.saveForecast(entity)
        }
    }

    fun saveForecasts(isEdite: Boolean, entities: List<Forecast>) = viewModelScope.launch {
        entities.forEach { entity ->
            if (isEdite) {
                repository.updateForecast(entity)
            } else {
                repository.saveForecast(entity)
            }
        }
    }


    private fun getAllDailyReportData() = viewModelScope.launch {
        repository.getAllDailyReportData().collect {
            _dailyReportDataList.postValue(DataStatus.success(it, false))
        }
    }

    fun saveDailyReportData(isEdite: Boolean, entity: DailyReportData) = viewModelScope.launch {
        if (isEdite) {
            repository.updateDailyReportData(entity)
        } else {
            repository.saveDailyReport(entity)
        }
    }

    private fun getAllIgnoredActivities() = viewModelScope.launch {
        repository.getIgnoredActivities().collect {
            _ignoredActivityList.postValue(DataStatus.success(it, false))
        }
    }

    fun saveIgnoredActivity(entity: IgnoredActivity) = viewModelScope.launch {
        repository.saveIgnoredActivity(entity)
    }

}