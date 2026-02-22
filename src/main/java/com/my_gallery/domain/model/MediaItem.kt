package com.my_gallery.domain.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName

@Keep
@Immutable
data class MediaItem(
    @SerializedName("id")
    val id: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("thumbnail")
    val thumbnail: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("dateAdded")
    val dateAdded: Long,
    @SerializedName("mimeType")
    val mimeType: String,
    @SerializedName("size")
    val size: Long = 0,
    @SerializedName("width")
    val width: Int = 0,
    @SerializedName("height")
    val height: Int = 0,
    @SerializedName("source")
    val source: String = "LOCAL",
    @SerializedName("path")
    val path: String? = null,
    @SerializedName("albumId")
    val albumId: String? = null,
    @SerializedName("originalAlbumId")
    val originalAlbumId: String? = null,
    @SerializedName("relativePath")
    val relativePath: String? = null,
    @SerializedName("rotation")
    val rotation: Float = 0f
)
@Keep
data class AlbumItem(
    val id: String,
    val name: String,
    val thumbnail: String,
    val count: Int,
    val rotation: Float = 0f
)
