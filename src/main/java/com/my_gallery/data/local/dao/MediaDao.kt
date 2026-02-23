package com.my_gallery.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.my_gallery.data.local.entity.MediaEntity
import kotlinx.coroutines.flow.Flow

data class MediaRotationTuple(
    val id: String,
    val rotation: Float
)

@Dao
interface MediaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaEntity>)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT id FROM media_items WHERE source = :source")
    suspend fun getAllIdsBySource(source: String): List<String>

    @Query("SELECT id, rotation FROM media_items WHERE source = :source")
    suspend fun getRotationsBySource(source: String): List<MediaRotationTuple>

    @Query("""
        SELECT * FROM media_items 
        WHERE source = :source 
        AND (:useDateFilter = 0 OR strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) IN (:periods))
        AND (
            :useTypeFilter = 0 
            OR (:includeImages = 1 AND mimeType LIKE 'image/%')
            OR (:includeVideos = 1 AND mimeType LIKE 'video/%')
        )
        AND (:useMimeFilter = 0 OR mimeType IN (:mimeTypes))
        AND (
            :useResFilter = 0 
            OR (:use4K = 1 AND (width >= 3840 OR height >= 3840))
            OR (:use2K = 1 AND ((width >= 2560 AND width < 3840) OR (height >= 2560 AND height < 3840)))
            OR (:use1080P = 1 AND ((width >= 1920 AND width < 2560) OR (height >= 1920 AND height < 2560)))
            OR (:use720P = 1 AND ((width >= 1280 AND width < 1920) OR (height >= 1280 AND height < 1920)))
            OR (:useSD = 1 AND (width < 1280 AND height < 1280))
        )
        AND (
            (:albumId IS NOT NULL AND albumId = :albumId) 
            OR (:albumId IS NULL AND albumId != 'SECURE_VAULT')
        )
        ORDER BY dateAdded DESC, id DESC
    """)
    fun pagingSourceMultiFilter(
        source: String,
        useDateFilter: Int,
        periods: List<String>,
        useTypeFilter: Int,
        includeImages: Int,
        includeVideos: Int,
        useMimeFilter: Int,
        mimeTypes: List<String>,
        useResFilter: Int,
        use4K: Int,
        use2K: Int,
        use1080P: Int,
        use720P: Int,
        useSD: Int,
        albumId: String? = null
    ): PagingSource<Int, MediaEntity>

    @Query("""
        SELECT id FROM media_items 
        WHERE source = :source 
        AND (:useDateFilter = 0 OR strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) IN (:periods))
        AND (
            :useTypeFilter = 0 
            OR (:includeImages = 1 AND mimeType LIKE 'image/%')
            OR (:includeVideos = 1 AND mimeType LIKE 'video/%')
        )
        AND (:useMimeFilter = 0 OR mimeType IN (:mimeTypes))
        AND (
            :useResFilter = 0 
            OR (:use4K = 1 AND (width >= 3840 OR height >= 3840))
            OR (:use2K = 1 AND ((width >= 2560 AND width < 3840) OR (height >= 2560 AND height < 3840)))
            OR (:use1080P = 1 AND ((width >= 1920 AND width < 2560) OR (height >= 1920 AND height < 2560)))
            OR (:use720P = 1 AND ((width >= 1280 AND width < 1920) OR (height >= 1280 AND height < 1920)))
            OR (:useSD = 1 AND (width < 1280 AND height < 1280))
        )
        AND (
            (:albumId IS NOT NULL AND albumId = :albumId) 
            OR (:albumId IS NULL AND albumId != 'SECURE_VAULT')
        )
        ORDER BY dateAdded DESC, id DESC
    """)
    suspend fun getMediaIdsMultiFilter(
        source: String,
        useDateFilter: Int,
        periods: List<String>,
        useTypeFilter: Int,
        includeImages: Int,
        includeVideos: Int,
        useMimeFilter: Int,
        mimeTypes: List<String>,
        useResFilter: Int,
        use4K: Int,
        use2K: Int,
        use1080P: Int,
        use720P: Int,
        useSD: Int,
        albumId: String? = null
    ): List<String>

    @Query("""
        SELECT 
            strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) as period,
            COUNT(*) as total,
            SUM(CASE WHEN mimeType LIKE 'image/%' THEN 1 ELSE 0 END) as images,
            SUM(CASE WHEN mimeType LIKE 'video/%' THEN 1 ELSE 0 END) as videos
        FROM media_items 
        WHERE source = :source 
        AND (
            :useTypeFilter = 0 
            OR (:includeImages = 1 AND mimeType LIKE 'image/%')
            OR (:includeVideos = 1 AND mimeType LIKE 'video/%')
        )
        AND (:useMimeFilter = 0 OR mimeType IN (:mimeTypes))
        AND (
            :useResFilter = 0 
            OR (:use4K = 1 AND (width >= 3840 OR height >= 3840))
            OR (:use2K = 1 AND ((width >= 2560 AND width < 3840) OR (height >= 2560 AND height < 3840)))
            OR (:use1080P = 1 AND ((width >= 1920 AND width < 2560) OR (height >= 1920 AND height < 2560)))
            OR (:use720P = 1 AND ((width >= 1280 AND width < 1920) OR (height >= 1280 AND height < 1920)))
            OR (:useSD = 1 AND (width < 1280 AND height < 1280))
        )
        AND (
            (:albumId IS NOT NULL AND albumId = :albumId) 
            OR (:albumId IS NULL AND albumId != 'SECURE_VAULT')
        )
        GROUP BY period
        ORDER BY MAX(dateAdded) DESC
    """)
    fun getAllSectionsMetadataMultiFilter(
        source: String, 
        useTypeFilter: Int,
        includeImages: Int,
        includeVideos: Int,
        useMimeFilter: Int,
        mimeTypes: List<String>,
        useResFilter: Int,
        use4K: Int,
        use2K: Int,
        use1080P: Int,
        use720P: Int,
        useSD: Int,
        albumId: String? = null
    ): Flow<List<SectionMetadataRow>>

    @Query("""
        SELECT COUNT(*) FROM media_items 
        WHERE source = :source 
        AND (:useDateFilter = 0 OR strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) IN (:periods))
        AND (
            :useTypeFilter = 0 
            OR (:includeImages = 1 AND mimeType LIKE 'image/%')
            OR (:includeVideos = 1 AND mimeType LIKE 'video/%')
        )
        AND (:useMimeFilter = 0 OR mimeType IN (:mimeTypes))
        AND (
            :useResFilter = 0 
            OR (:use4K = 1 AND (width >= 3840 OR height >= 3840))
            OR (:use2K = 1 AND ((width >= 2560 AND width < 3840) OR (height >= 2560 AND height < 3840)))
            OR (:use1080P = 1 AND ((width >= 1920 AND width < 2560) OR (height >= 1920 AND height < 2560)))
            OR (:use720P = 1 AND ((width >= 1280 AND width < 1920) OR (height >= 1280 AND height < 1920)))
            OR (:useSD = 1 AND (width < 1280 AND height < 1280))
        )
        AND (
            (:albumId IS NOT NULL AND albumId = :albumId) 
            OR (:albumId IS NULL AND albumId != 'SECURE_VAULT')
        )
        AND (
            dateAdded > (SELECT dateAdded FROM media_items WHERE id = :targetId)
            OR (dateAdded = (SELECT dateAdded FROM media_items WHERE id = :targetId) AND id > :targetId)
        )
    """)
    suspend fun getMediaRankMultiFilter(
        targetId: String,
        source: String,
        useDateFilter: Int,
        periods: List<String>,
        useTypeFilter: Int,
        includeImages: Int,
        includeVideos: Int,
        useMimeFilter: Int,
        mimeTypes: List<String>,
        useResFilter: Int,
        use4K: Int,
        use2K: Int,
        use1080P: Int,
        use720P: Int,
        useSD: Int,
        albumId: String? = null
    ): Int
    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getMediaByIds(ids: List<String>): List<MediaEntity>

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

    @Query("DELETE FROM media_items WHERE source = :source AND (albumId IS NULL OR albumId != 'SECURE_VAULT')")
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
        SELECT id FROM media_items 
        WHERE source = :source 
        AND strftime('%m-%Y', datetime(dateAdded/1000, 'unixepoch')) = :period
        AND mimeType LIKE :mimeType 
        AND (
            (:albumId IS NOT NULL AND albumId = :albumId) 
            OR (:albumId IS NULL AND albumId != 'SECURE_VAULT')
        )
    """)
    suspend fun getMediaIdsByPeriod(
        source: String,
        period: String,
        mimeType: String,
        albumId: String? = null
    ): List<String>
}

data class SectionMetadataRow(
    val period: String, // format "MM-YYYY"
    val total: Int, 
    val images: Int, 
    val videos: Int
)

data class SectionMetadata(val total: Int, val images: Int, val videos: Int)

data class MediaResolution(val width: Int, val height: Int)
