package de.drtobiasprinz.summitbook.di

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import de.drtobiasprinz.summitbook.repository.DatabaseRepository
import de.drtobiasprinz.summitbook.viewmodel.StatisticsViewModel

@Module
@InstallIn(ActivityRetainedComponent::class)
object ViewModelModule {

    @Provides
    @ActivityRetainedScoped
    fun provideStatisticsViewModelFactory(
        repository: DatabaseRepository,
        sharedPreferences: SharedPreferences
    ): ViewModelProvider.Factory {
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return StatisticsViewModel(repository, sharedPreferences) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}