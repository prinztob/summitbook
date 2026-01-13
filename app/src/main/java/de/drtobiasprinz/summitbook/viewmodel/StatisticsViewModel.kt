package de.drtobiasprinz.summitbook.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.drtobiasprinz.summitbook.Keys
import de.drtobiasprinz.summitbook.models.StatisticsData
import de.drtobiasprinz.summitbook.models.StatisticEntry
import de.drtobiasprinz.summitbook.ui.utils.ExtremaValuesSummits
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: DatabaseRepository,
    sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _statisticsData = MutableStateFlow(StatisticsData())
    val statisticsData: StateFlow<StatisticsData> = _statisticsData.asStateFlow()

    private var annualTargetActivity: String = ""
    private var annualTargetKm: String = ""
    private var annualTargetHm: String = ""
    private var indoorHeightMeterPercent: Int = 0

    init {
        // Get values from SharedPreferences
        annualTargetActivity = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_ACTIVITIES, "52") ?: "52"
        annualTargetKm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET_KM, "1200") ?: "1200"
        annualTargetHm = sharedPreferences.getString(Keys.PREF_ANNUAL_TARGET, "50000") ?: "50000"
        indoorHeightMeterPercent = sharedPreferences.getInt(Keys.PREF_INDOOR_HEIGHT_METER, 0)
    }

    fun updateStatistics() {
        viewModelScope.launch {
            try {
                val summitsFlow = repository.getAllSummits()
                val forecastsFlow = repository.getAllForecasts()
                
                withContext(Dispatchers.IO) {
                    // Collect the flows to get the actual data
                    val summits = summitsFlow.first()
                    val forecasts = forecastsFlow.first()
                    
                    val statisticEntry = StatisticEntry(
                        summits,
                        annualTargetActivity.toIntOrNull() ?: 52,
                        annualTargetKm.toIntOrNull() ?: 1200,
                        annualTargetHm.toIntOrNull() ?: 50000,
                        indoorHeightMeterPercent
                    )
                    statisticEntry.calculate()
                    
                    val extremaValuesSummits = ExtremaValuesSummits(
                        summits, shouldIndoorActivityBeExcluded = true
                    )
                    
                    val newData = StatisticsData(
                        totalActivities = statisticEntry.getTotalActivities(),
                        totalSummits = statisticEntry.getTotalSummits(),
                        totalKm = statisticEntry.totalKm,
                        totalHm = statisticEntry.totalHm,
                        achievement = statisticEntry.getAchievement(),
                        visitedCountries = statisticEntry.getVisitedCountries(),
                        totalRoadSurfaceMeter = statisticEntry.totalRoadSurfaceMeter,
                        totalRoadTypeMeter = statisticEntry.totalRoadTypeMeter,
                        extremaValuesSummits = extremaValuesSummits,
                        forecasts = forecasts,
                        summits = summits
                    )
                    
                    _statisticsData.value = newData
                }
            } catch (e: Exception) {
                // Handle error appropriately
                e.printStackTrace()
            }
        }
    }
}