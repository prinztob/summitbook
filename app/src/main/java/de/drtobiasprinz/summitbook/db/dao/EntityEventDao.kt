package de.drtobiasprinz.summitbook.db.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.db.entities.EntityEvent
import kotlinx.coroutines.flow.Flow


@Dao
interface EntityEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entityEvent: EntityEvent)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entityEvent: EntityEvent)

    @Transaction
    @Query("select * from entityevent")
    fun getAllEntityEvents(): Flow<List<EntityEvent>>

    @Delete
    suspend fun delete(entityEvent: EntityEvent)

}