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
        AND (:albumId IS NULL OR albumId = :albumId)
        ORDER BY dateAdded DESC
    """)
    fun pagingSourceAdvanced(
        source: String, 
        start: Long, 
        end: Long, 
        mimeType: String,
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): PagingSource<Int, MediaEntity>

    @Query("""
        SELECT id FROM media_items 
        WHERE source = :source 
        AND dateAdded >= :start AND dateAdded < :end 
        AND mimeType LIKE :mimeType 
        AND (
            (width >= :minWidth AND height >= :minHeight) OR (width >= :minHeight AND height >= :minWidth)
        )
        AND (:albumId IS NULL OR albumId = :albumId)
    """)
    suspend fun getMediaIds(
        source: String, 
        start: Long, 
        end: Long, 
        mimeType: String, 
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): List<String>

    @Query("SELECT DISTINCT mimeType FROM media_items WHERE source = :source")
    fun getDistinctMimeTypes(source: String): Flow<List<String>>

    @Query("SELECT DISTINCT width, height FROM media_items WHERE source = :source AND mimeType LIKE 'video/%'")
    fun getDistinctVideoResolutions(source: String): Flow<List<MediaResolution>>

    @Query("SELECT COUNT(*) FROM media_items WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Query("UPDATE media_items SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE media_items SET path = :newPath, albumId = :newAlbumId WHERE id = :id")
    suspend fun updatePathAndAlbum(id: String, newPath: String, newAlbumId: String)

    @Query("DELETE FROM media_items WHERE source = :source")
    suspend fun clearBySource(source: String)

    @Query("DELETE FROM media_items")
    suspend fun clearAll()

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("""
        SELECT 
            strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) as period,
            COUNT(*) as total,
            SUM(CASE WHEN mimeType LIKE 'image/%' THEN 1 ELSE 0 END) as images,
            SUM(CASE WHEN mimeType LIKE 'video/%' THEN 1 ELSE 0 END) as videos
        FROM media_items 
        WHERE source = :source 
        AND mimeType LIKE :mimeType 
        AND (
            (width >= :minWidth AND height >= :minHeight) OR (width >= :minHeight AND height >= :minWidth)
        )
        AND (:albumId IS NULL OR albumId = :albumId)
        GROUP BY period
    """)
    fun getAllSectionsMetadata(
        source: String, 
        mimeType: String,
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): Flow<List<SectionMetadataRow>>
}

data class SectionMetadataRow(
    val period: String, // format "MM-YYYY"
    val total: Int, 
    val images: Int, 
    val videos: Int
)

data class SectionMetadata(val total: Int, val images: Int, val videos: Int)

data class MediaResolution(val width: Int, val height: Int)
