package com.my_gallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.my_gallery.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavoriteFlow(id: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM favorites")
    fun getCountFlow(): Flow<Int>

    @Query("SELECT media_items.url FROM media_items INNER JOIN favorites ON media_items.id = favorites.id ORDER BY favorites.dateAdded DESC LIMIT 1")
    suspend fun getLatestThumbnail(): String?

    @Query("SELECT media_items.url FROM media_items INNER JOIN favorites ON media_items.id = favorites.id ORDER BY favorites.dateAdded DESC LIMIT 1")
    fun getLatestThumbnailFlow(): Flow<String?>
}
