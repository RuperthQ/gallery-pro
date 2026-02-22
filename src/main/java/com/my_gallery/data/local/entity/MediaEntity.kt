package com.my_gallery.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.my_gallery.domain.model.MediaItem

@Keep
@Entity(tableName = "media_items", indices = [androidx.room.Index(value = ["dateAdded"]), androidx.room.Index(value = ["albumId"])])
data class MediaEntity(
    @PrimaryKey val id: String,
    val url: String,
    val thumbnail: String,
    val title: String,
    val dateAdded: Long,
    val mimeType: String,
    val size: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val source: String = "LOCAL",
    val path: String? = null,
    val albumId: String? = null,
    val originalAlbumId: String? = null,
    val relativePath: String? = null,
    val rotation: Float = 0f
) {
    fun toDomain() = MediaItem(
        id = id,
        url = url,
        thumbnail = thumbnail,
        title = title,
        dateAdded = dateAdded,
        mimeType = mimeType,
        size = size,
        width = width,
        height = height,
        source = source,
        path = path,
        albumId = albumId,
        originalAlbumId = originalAlbumId,
        relativePath = relativePath,
        rotation = rotation
    )

    companion object {
        fun fromDomain(domain: MediaItem, source: String) = MediaEntity(
            id = domain.id,
            url = domain.url,
            thumbnail = domain.thumbnail,
            title = domain.title,
            dateAdded = domain.dateAdded,
            mimeType = domain.mimeType,
            size = domain.size,
            width = domain.width,
            height = domain.height,
            source = source,
            path = domain.path,
            albumId = domain.albumId,
            originalAlbumId = domain.originalAlbumId,
            relativePath = domain.relativePath,
            rotation = domain.rotation
        )
    }
}
