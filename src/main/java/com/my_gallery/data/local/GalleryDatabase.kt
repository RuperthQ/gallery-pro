package com.my_gallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.my_gallery.data.local.dao.MediaDao
import com.my_gallery.data.local.entity.MediaEntity

@Database(
    entities = [MediaEntity::class],
    version = 8,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
