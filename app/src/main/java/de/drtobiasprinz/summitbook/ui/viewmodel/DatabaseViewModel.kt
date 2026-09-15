package de.drtobiasprinz.summitbook.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.drtobiasprinz.summitbook.core.DataStatus
import de.drtobiasprinz.summitbook.core.widget.WidgetUpdater
import de.drtobiasprinz.summitbook.data.appstate.AppState
import de.drtobiasprinz.summitbook.data.db.entities.DailyActivitySummary
import de.drtobiasprinz.summitbook.data.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.data.db.entities.Forecast
import de.drtobiasprinz.summitbook.data.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.data.db.entities.Peak
import de.drtobiasprinz.summitbook.data.db.entities.Segment
import de.drtobiasprinz.summitbook.data.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.data.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.data.db.entities.Summit
import de.drtobiasprinz.summitbook.data.repository.DatabaseRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseViewModel @Inject constructor(
    private val repository: DatabaseRepository,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private var _summitsList = MutableLiveData<DataStatus<List<Summit>>>()
    val summitsList: LiveData<DataStatus<List<Summit>>>
        get() = _summitsList

    private val _segmentsList = MutableLiveData<DataStatus<List<Segment>>>()
    val segmentsList: LiveData<DataStatus<List<Segment>>>
        get() = _segmentsList

    private val _forecastsList = MutableLiveData<DataStatus<List<Forecast>>>()
    val forecastList: LiveData<DataStatus<List<Forecast>>>
        get() = _forecastsList

    private val _ignoredActivityList = MutableLiveData<DataStatus<List<IgnoredActivity>>>()
    val ignoredActivityList: LiveData<DataStatus<List<IgnoredActivity>>>
        get() = _ignoredActivityList

    private val _peaks = MutableLiveData<DataStatus<List<Peak>>>()
    val peaks: LiveData<DataStatus<List<Peak>>>
        get() = _peaks

    private var _entityEvents = MutableLiveData<DataStatus<List<EntityEvent>>>()
    val entityEvents: LiveData<DataStatus<List<EntityEvent>>>
        get() = _entityEvents

    private val _dailyActivitySummaryList =
        MutableLiveData<DataStatus<List<DailyActivitySummary>>>()
    val dailyActivitySummary: LiveData<DataStatus<List<DailyActivitySummary>>>
        get() = _dailyActivitySummaryList

    private val _mountainPasses = MutableLiveData<DataStatus<List<SegmentEntry>>>()
    val mountainPasses: LiveData<DataStatus<List<SegmentEntry>>>
        get() = _mountainPasses

    init {
        getAllSummits()
        getAllSegments()
        getAllForecasts()
        getAllIgnoredActivities()
        getPeaks()
        getAllEntityEvent()
        getAllDailyActivitySummaries()
        getAllMountainPasses()
    }

    fun refresh() {
        _summitsList.value = _summitsList.value
        _segmentsList.value = _segmentsList.value
    }

    fun saveSummit(isEdite: Boolean, entity: Summit) = viewModelScope.launch {
        entity.places = entity.places.filter { it.isNotEmpty() }
        entity.countries = entity.countries.filter { it.isNotEmpty() }
        entity.participants = entity.participants.filter { it.isNotEmpty() }
        entity.equipments = entity.equipments.filter { it.isNotEmpty() }
        if (isEdite) {
            repository.updateSummit(entity)
        } else {
            entity.id = repository.saveSummit(entity)
        }
        updateWidget()
    }

    fun updatePeakName(oldName: String, newName: String) = viewModelScope.launch {
        val peak = repository.getPeakByName(oldName)
        if (peak != null) {
            peak.name = newName
            repository.updatePeak(peak)
        }
    }

    fun saveSummits(entities: List<Summit>) = viewModelScope.launch {
        entities.forEach { entity ->
            entity.places = entity.places.filter { it.isNotEmpty() }
            entity.countries = entity.countries.filter { it.isNotEmpty() }
            entity.participants = entity.participants.filter { it.isNotEmpty() }
            entity.equipments = entity.equipments.filter { it.isNotEmpty() }
        }
        repository.saveSummits(entities)
        updateWidget()
    }

    fun updateSummits(entities: List<Summit>) = viewModelScope.launch {
        repository.updateSummits(entities)
        updateWidget()
    }

    fun deleteSummit(entity: Summit) = viewModelScope.launch {
        repository.deleteSummit(entity)
        updateWidget()
    }

    private fun updateWidget() {
        viewModelScope.launch {
            widgetUpdater.updateWidget()
        }
    }

    fun getAllSummits() = viewModelScope.launch {
        _summitsList.postValue(DataStatus.loading())
        repository.getAllSummits()
            .catch { _summitsList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _summitsList.postValue(DataStatus.success(it, it.isEmpty())) }
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

    fun getAllForecasts() = viewModelScope.launch {
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
        updateWidget()
    }

    fun saveForecasts(isEdite: Boolean, entities: List<Forecast>) = viewModelScope.launch {
        entities.forEach { entity ->
            if (isEdite) {
                repository.updateForecast(entity)
            } else {
                repository.saveForecast(entity)
            }
        }
        updateWidget()
    }

    private fun getAllIgnoredActivities() = viewModelScope.launch {
        repository.getIgnoredActivities().collect {
            _ignoredActivityList.postValue(DataStatus.success(it, false))
        }
    }

    fun saveIgnoredActivity(entity: IgnoredActivity) = viewModelScope.launch {
        repository.saveIgnoredActivity(entity)
    }

    private fun getPeaks() = viewModelScope.launch {
        repository.getPeaks().collect {
            _peaks.postValue(DataStatus.success(it, false))
            AppState.peaks = it.toMutableList()
        }
    }

    fun savePeak(peak: Peak) = viewModelScope.launch {
        repository.savePeak(peak)
    }

    fun deletePeak(peak: Peak) = viewModelScope.launch {
        repository.deletePeak(peak)
    }

    fun saveEntityEvent(isEdite: Boolean, entity: EntityEvent) = viewModelScope.launch {
        if (isEdite) {
            repository.updateEntityEvent(entity)
        } else {
            repository.saveEntityEvent(entity)
        }
    }

    fun deleteEntityEvent(entity: EntityEvent) = viewModelScope.launch {
        repository.deleteEntityEvent(entity)
    }

    private fun getAllEntityEvent() = viewModelScope.launch {
        _entityEvents.postValue(DataStatus.loading())
        repository.getEntityEvents()
            .catch { _entityEvents.postValue(DataStatus.error(it.message.toString())) }
            .collect { _entityEvents.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun getAllDailyActivitySummaries() = viewModelScope.launch {
        _dailyActivitySummaryList.postValue(DataStatus.loading())
        repository.getAllDailyActivitySummary()
            .catch { _dailyActivitySummaryList.postValue(DataStatus.error(it.message.toString())) }
            .collect { _dailyActivitySummaryList.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun saveActivitySummary(entity: DailyActivitySummary) = viewModelScope.launch {
        repository.saveDailyActivitySummary(entity)
    }

    private fun getAllMountainPasses() = viewModelScope.launch {
        repository.getAllMountainPasses()
            .catch { _mountainPasses.postValue(DataStatus.error(it.message.toString())) }
            .collect { _mountainPasses.postValue(DataStatus.success(it, it.isEmpty())) }
    }

    fun saveMountainPass(mountainPass: SegmentEntry) = viewModelScope.launch {
        repository.saveSegmentEntry(mountainPass)
    }


    fun deleteMountainPass(mountainPass: SegmentEntry) = viewModelScope.launch {
        repository.deleteSegmentEntry(mountainPass)
    }

}