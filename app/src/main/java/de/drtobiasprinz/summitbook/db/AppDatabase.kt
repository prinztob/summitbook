package de.drtobiasprinz.summitbook.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.drtobiasprinz.summitbook.db.dao.*
import de.drtobiasprinz.summitbook.db.entities.*


@Database(entities = [Summit::class, Forecast::class, IgnoredActivity::class,
    SegmentDetails::class, SegmentEntry::class, SolarIntensity::class],
    version = 2,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summitsDao(): SummitsDao
    abstract fun forecastDao(): ForecastDao?
    abstract fun segmentsDao(): SegmentsDao?
    abstract fun solarIntensityDao(): SolarIntensityDao?
    abstract fun ignoredActivityDao(): IgnoredActivityDao?
}