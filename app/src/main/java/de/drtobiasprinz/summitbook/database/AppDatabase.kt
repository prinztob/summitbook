package de.drtobiasprinz.summitbook.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import de.drtobiasprinz.summitbook.dao.*
import de.drtobiasprinz.summitbook.models.Forecast
import de.drtobiasprinz.summitbook.models.IgnoredActivity
import de.drtobiasprinz.summitbook.models.Summit


@Database(
        entities = [Summit::class, Forecast::class, IgnoredActivity::class],
        version = 5,
        autoMigrations = [
            AutoMigration(from = 1, to = 2),
            AutoMigration(from = 2, to = 3),
            AutoMigration(from = 3, to = 4, spec = AppDatabase.MyAutoMigration::class),
            AutoMigration(from = 4, to = 5)
        ],
        exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forecastDao(): ForecastDao?
    abstract fun summitDao(): SummitDao?
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
}