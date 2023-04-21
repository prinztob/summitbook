package de.drtobiasprinz.summitbook.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.drtobiasprinz.summitbook.adapter.ContactsAdapter
import de.drtobiasprinz.summitbook.db.AppDatabase
import de.drtobiasprinz.summitbook.models.SortFilterValues
import de.drtobiasprinz.summitbook.db.entities.Summit
import de.drtobiasprinz.summitbook.utils.Constants.DATABASE
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
    fun provideSummitsDao(db: AppDatabase) = db.summitsDao()

    @Provides
    @Singleton
    fun provideSegmentsDao(db: AppDatabase) = db.segmentsDao()

    @Provides
    @Singleton
    fun provideSortFilterValues() = SortFilterValues()

    @Provides
    @Singleton
    fun provideContactsAdapter() = ContactsAdapter()

    @Provides
    fun provideEntity() = Summit()

}