package de.drtobiasprinz.summitbook.dao

import androidx.room.*
import de.drtobiasprinz.summitbook.models.*


@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addBookmark(bookmark: Bookmark?): Long

    @get:Query("select * from bookmark")
    val allBookmark: List<Bookmark>?

    @Query("select * from bookmark where id = :id")
    fun getBookmark(id: Long): Bookmark?

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateBookmark(bookmark: Bookmark?)

    @Query("delete from bookmark")
    fun removeAllBookmark()

    @Delete
    fun delete(bookmark: Bookmark?)
}