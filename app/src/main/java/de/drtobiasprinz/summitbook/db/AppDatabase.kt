package de.drtobiasprinz.summitbook.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.drtobiasprinz.summitbook.db.dao.EntityEventDao
import de.drtobiasprinz.summitbook.db.dao.ForecastDao
import de.drtobiasprinz.summitbook.db.dao.IgnoredActivityDao
import de.drtobiasprinz.summitbook.db.dao.PeakDao
import de.drtobiasprinz.summitbook.db.dao.SegmentsDao
import de.drtobiasprinz.summitbook.db.dao.SummitsDao
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.db.entities.Peak
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit


@Database(
    entities = [Summit::class, Forecast::class, IgnoredActivity::class,
        SegmentDetails::class, SegmentEntry::class, EntityEvent::class, Peak::class],
    version = 6,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
    ]
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summitsDao(): SummitsDao
    abstract fun forecastDao(): ForecastDao
    abstract fun segmentsDao(): SegmentsDao
    abstract fun ignoredActivityDao(): IgnoredActivityDao
    abstract fun peakDao(): PeakDao
    abstract fun entityEventDao(): EntityEventDao
}