package de.drtobiasprinz.summitbook.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class IgnoredActivity(
        var activityId: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}