package de.drtobiasprinz.summitbook.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.drtobiasprinz.summitbook.db.dao.*
import de.drtobiasprinz.summitbook.db.entities.*


@Database(
    entities = [Summit::class, Forecast::class, IgnoredActivity::class,
        SegmentDetails::class, SegmentEntry::class, DailyReportData::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6)
    ]
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summitsDao(): SummitsDao
    abstract fun forecastDao(): ForecastDao
    abstract fun segmentsDao(): SegmentsDao
    abstract fun dailyReportDataDao(): DailyReportDataDao
    abstract fun ignoredActivityDao(): IgnoredActivityDao
}