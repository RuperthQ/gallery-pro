package com.my_gallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.dao.FavoriteDao
import com.my_gallery.data.local.entity.MediaEntity
import com.my_gallery.data.local.entity.FavoriteEntity

@Database(
    entities = [MediaEntity::class, FavoriteEntity::class],
    version = 11,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun favoriteDao(): FavoriteDao
}
