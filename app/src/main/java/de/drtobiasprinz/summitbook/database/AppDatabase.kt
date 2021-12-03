package de.drtobiasprinz.summitbook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.AutoMigration
import androidx.room.TypeConverters
import de.drtobiasprinz.summitbook.dao.*
import de.drtobiasprinz.summitbook.models.Bookmark
import de.drtobiasprinz.summitbook.models.Forecast
import de.drtobiasprinz.summitbook.models.IgnoredActivity
import de.drtobiasprinz.summitbook.models.Summit


@Database(
        entities = [Summit::class, Bookmark::class, Forecast::class, IgnoredActivity::class],
        version = 2,
        autoMigrations = [
            AutoMigration (from = 1, to = 2)
        ],
        exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao?
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
}