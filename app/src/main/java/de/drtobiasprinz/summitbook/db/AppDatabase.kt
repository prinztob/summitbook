package de.drtobiasprinz.summitbook.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import de.drtobiasprinz.summitbook.db.dao.DailyActivitySummaryDao
import de.drtobiasprinz.summitbook.db.dao.EntityEventDao
import de.drtobiasprinz.summitbook.db.dao.ForecastDao
import de.drtobiasprinz.summitbook.db.dao.IgnoredActivityDao
import de.drtobiasprinz.summitbook.db.dao.PeakDao
import de.drtobiasprinz.summitbook.db.dao.SegmentDao
import de.drtobiasprinz.summitbook.db.dao.SummitsDao
import de.drtobiasprinz.summitbook.db.entities.DailyActivitySummary
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import de.drtobiasprinz.summitbook.db.entities.Forecast
import de.drtobiasprinz.summitbook.db.entities.IgnoredActivity
import de.drtobiasprinz.summitbook.db.entities.Peak
import de.drtobiasprinz.summitbook.db.entities.SegmentDetails
import de.drtobiasprinz.summitbook.db.entities.SegmentEntry
import de.drtobiasprinz.summitbook.db.entities.Summit


@Database(
    entities = [Summit::class, Forecast::class, IgnoredActivity::class,
        SegmentDetails::class, SegmentEntry::class, EntityEvent::class, Peak::class, DailyActivitySummary::class],
    version = 13,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9, spec = AppDatabase.AutoMigration8to9::class),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = AppDatabase.AutoMigration10to11::class),
        AutoMigration(from = 11, to = 12, spec = AppDatabase.AutoMigration11to12::class),
        AutoMigration(from = 12, to = 13),
    ]
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun summitsDao(): SummitsDao
    abstract fun forecastDao(): ForecastDao
    abstract fun segmentDao(): SegmentDao
    abstract fun ignoredActivityDao(): IgnoredActivityDao
    abstract fun peakDao(): PeakDao
    abstract fun entityEventDao(): EntityEventDao
    abstract fun dailyActivitySummaryDao(): DailyActivitySummaryDao

    @RenameColumn("daily_report", "countOfActivities", "activityId")
    class AutoMigration8to9 : AutoMigrationSpec

    @DeleteTable(tableName = "MountainPass")
    class AutoMigration10to11 : AutoMigrationSpec

    @DeleteColumn.Entries(
        DeleteColumn(tableName = "SegmentEntry", columnName = "elevationGain"),
        DeleteColumn(tableName = "SegmentEntry", columnName = "elevationLoss")
    )
    class AutoMigration11to12 : AutoMigrationSpec
}