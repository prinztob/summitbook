package de.drtobiasprinz.summitbook.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import de.drtobiasprinz.summitbook.dao.*
import de.drtobiasprinz.summitbook.models.*


@Database(
        entities = [Summit::class, Forecast::class, IgnoredActivity::class, SegmentDetails::class, SegmentEntry::class, SolarIntensity::class],
        version = 8,
        autoMigrations = [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4, spec = AppDatabase.MyAutoMigration::class),
            AutoMigration(from = 4, to = 5),
            AutoMigration(from = 5, to = 6),
            AutoMigration(from = 6, to = 7),
            AutoMigration(from = 7, to = 8, spec = AppDatabase.MyAutoMigration7To8::class)
        ],
        exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forecastDao(): ForecastDao?
    abstract fun summitDao(): SummitDao?
    abstract fun segmentsDao(): SegmentsDao?
    abstract fun solarIntensityDao(): SolarIntensityDao?
    abstract fun ignoredActivityDao(): IgnoredActivityDao?

    companion object {
        private lateinit var database: AppDatabase
        fun getDatabase(context: Context): AppDatabase {
            database = Room.databaseBuilder(context, AppDatabase::class.java, "summitdatabase")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            return database
        }
    }
    @DeleteTable(tableName = "Bookmark")
    class MyAutoMigration : AutoMigrationSpec

    @RenameColumn(
            tableName = "SolarIntensity",
            fromColumnName = "solarIntensityInBatteryPerCent",
            toColumnName = "solarUtilizationInHours"
    )
    class MyAutoMigration7To8 : AutoMigrationSpec

}