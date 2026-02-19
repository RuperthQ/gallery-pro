package com.my_gallery.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.my_gallery.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)

    @Query("""
        SELECT * FROM media_items 
        WHERE source = :source 
        AND dateAdded >= :start AND dateAdded < :end 
        AND mimeType LIKE :mimeType 
        AND (
            (width >= :minWidth AND height >= :minHeight) OR (width >= :minHeight AND height >= :minWidth)
        )
        ORDER BY dateAdded DESC
    """)
    fun pagingSourceAdvanced(
        source: String, 
        start: Long, 
        end: Long, 
        mimeType: String,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): PagingSource<Int, MediaEntity>

    @Query("SELECT DISTINCT mimeType FROM media_items WHERE source = :source")
    fun getDistinctMimeTypes(source: String): Flow<List<String>>

    @Query("SELECT DISTINCT width, height FROM media_items WHERE source = :source AND mimeType LIKE 'video/%'")
    fun getDistinctVideoResolutions(source: String): Flow<List<MediaResolution>>

    @Query("SELECT COUNT(*) FROM media_items WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}

data class MediaResolution(val width: Int, val height: Int)
