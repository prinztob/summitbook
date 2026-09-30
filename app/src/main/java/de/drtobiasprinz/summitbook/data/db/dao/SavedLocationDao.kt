package de.drtobiasprinz.summitbook.data.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.data.db.entities.SavedLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(location: SavedLocation)

    @Update
    suspend fun update(location: SavedLocation)

    @Delete
    suspend fun delete(location: SavedLocation)

    @Query("select * from SavedLocation")
    fun getAllSavedLocations(): Flow<List<SavedLocation>>
}
