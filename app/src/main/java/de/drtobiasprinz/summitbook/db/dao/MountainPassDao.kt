package de.drtobiasprinz.summitbook.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.drtobiasprinz.summitbook.db.entities.MountainPass
import kotlinx.coroutines.flow.Flow

@Dao
interface MountainPassDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(mountainPass: MountainPass): Long

    @Update
    suspend fun update(mountainPass: MountainPass)

    @Delete
    suspend fun delete(mountainPass: MountainPass)

    @Query("SELECT * FROM MountainPass")
    fun getAllMountainPasses(): Flow<List<MountainPass>>

    @Query("SELECT * FROM MountainPass WHERE activityId = :activityId")
    fun getMountainPassesForActivity(activityId: Long): Flow<List<MountainPass>>

    @Query("SELECT * FROM MountainPass WHERE id = :id")
    suspend fun getMountainPassById(id: Long): MountainPass?
}
