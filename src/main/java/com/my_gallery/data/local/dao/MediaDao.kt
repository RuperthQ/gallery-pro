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

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT id FROM media_items WHERE source = :source")
    suspend fun getAllIdsBySource(source: String): List<String>

    @Query("""
        SELECT * FROM media_items 
        WHERE source = :source 
        AND dateAdded >= :start AND dateAdded < :end 
        AND mimeType LIKE :mimeType 
        AND (
            (width >= :minWidth AND height >= :minHeight) OR (width >= :minHeight AND height >= :minWidth)
        )
        AND (:albumId IS NOT NULL AND albumId = :albumId OR :albumId IS NULL AND albumId != 'SECURE_VAULT')
        ORDER BY dateAdded DESC, id DESC
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
        SELECT COUNT(*) FROM media_items 
        WHERE source = :source 
        AND dateAdded >= :start AND dateAdded < :end 
        AND mimeType LIKE :mimeType 
        AND (
            (width >= :minWidth AND height >= :minHeight) OR (width >= :minHeight AND height >= :minWidth)
        )
        AND (:albumId IS NOT NULL AND albumId = :albumId OR :albumId IS NULL AND albumId != 'SECURE_VAULT')
        AND (
            dateAdded > (SELECT dateAdded FROM media_items WHERE id = :targetId)
            OR (dateAdded = (SELECT dateAdded FROM media_items WHERE id = :targetId) AND id > :targetId)
        )
    """)
    suspend fun getMediaRank(
        targetId: String,
        source: String, 
        start: Long, 
        end: Long, 
        mimeType: String, 
        albumId: String? = null,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): Int

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaByIds(ids: List<String>): List<MediaEntity>

    @Query("""
        SELECT id FROM media_items 
        WHERE source = :source 
        AND strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) = :period
        AND mimeType LIKE :mimeType 
        AND (:albumId IS NOT NULL AND albumId = :albumId OR :albumId IS NULL AND albumId != 'SECURE_VAULT')
    """)
    suspend fun getMediaIdsByPeriod(
        source: String,
        period: String,
        mimeType: String,
        albumId: String? = null
    ): List<String>

    @Query("""SELECT id FROM media_items
        WHERE source = :source 
        AND dateAdded >= :start AND dateAdded < :end 
        AND mimeType LIKE :mimeType 
        AND (
            (width >= :minWidth AND height >= :minHeight) OR (width >= :minHeight AND height >= :minWidth)
        )
        AND (:albumId IS NOT NULL AND albumId = :albumId OR :albumId IS NULL AND albumId != 'SECURE_VAULT')
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

    @Query("UPDATE media_items SET rotation = :rotation WHERE id = :id")
    suspend fun updateRotation(id: String, rotation: Float)

    @Query("UPDATE media_items SET path = :newPath, albumId = :newAlbumId WHERE id = :id")
    suspend fun updatePathAndAlbum(id: String, newPath: String, newAlbumId: String)

    @Query("UPDATE media_items SET path = :newPath, albumId = :newAlbumId, url = :newUrl, thumbnail = :newUrl, originalAlbumId = :oldAlbumId, relativePath = :relativePath WHERE id = :id")
    suspend fun updatePathAlbumAndUrl(id: String, newPath: String, newAlbumId: String, newUrl: String, oldAlbumId: String?, relativePath: String?)

    @Query("DELETE FROM media_items WHERE source = :source AND albumId != 'SECURE_VAULT'")
    suspend fun clearBySource(source: String)

    @Query("SELECT COUNT(*) FROM media_items WHERE albumId = 'SECURE_VAULT'")
    suspend fun getVaultCount(): Int

    @Query("SELECT url FROM media_items WHERE albumId = 'SECURE_VAULT' ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getVaultLatestThumbnail(): String?

    @Query("SELECT rotation FROM media_items WHERE url = :url LIMIT 1")
    suspend fun getRotationByUrl(url: String): Float?

    @Query("SELECT id FROM media_items WHERE albumId = 'SECURE_VAULT'")
    suspend fun getVaultIds(): List<String>

    @Query("SELECT thumbnail FROM media_items WHERE albumId != 'SECURE_VAULT' ORDER BY dateAdded DESC LIMIT 1")
    suspend fun getLatestPublicThumbnail(): String?

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
        AND (:albumId IS NOT NULL AND albumId = :albumId OR :albumId IS NULL AND albumId != 'SECURE_VAULT')
        GROUP BY period
        ORDER BY MAX(dateAdded) DESC
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
