package de.drtobiasprinz.summitbook.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.drtobiasprinz.summitbook.dao.BookmarkDao
import de.drtobiasprinz.summitbook.dao.Converters
import de.drtobiasprinz.summitbook.dao.IgnoredActivityDao
import de.drtobiasprinz.summitbook.dao.SummitDao
import de.drtobiasprinz.summitbook.models.Bookmark
import de.drtobiasprinz.summitbook.models.IgnoredActivity
import de.drtobiasprinz.summitbook.models.Summit


@Database(entities = [Summit::class, Bookmark::class, IgnoredActivity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao?
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