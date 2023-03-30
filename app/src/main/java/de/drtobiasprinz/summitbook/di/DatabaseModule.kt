package de.drtobiasprinz.summitbook.di

import android.content.Context
import androidx.room.Room
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.utils.Constants.DATABASE
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.db.entities.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.ui.GarminPythonExecutor
import de.drtobiasprinz.summitbook.ui.MainActivity
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context) = Room.databaseBuilder(
        context, AppDatabase::class.java, DATABASE
    ).allowMainThreadQueries()
        .fallbackToDestructiveMigration().build()


    @Provides
    @Singleton
    fun provideDao(db: AppDatabase) = db.summitsDao()

    @Provides
    @Singleton
    fun provideSortFilterValues() = SortFilterValues()

    @Provides
    @Singleton
    fun provideContactsAdapter() = ContactsAdapter()

    @Provides
    @Singleton
    fun providePythonExecutor() = GarminPythonExecutor(MainActivity.pythonInstance, "", "")

    @Provides
    fun provideEntity()= Summit()

}