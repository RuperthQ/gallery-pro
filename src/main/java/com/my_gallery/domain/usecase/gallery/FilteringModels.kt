package com.my_gallery.domain.usecase.gallery

data class FilterMetadataParams(
    val mimeTypes: List<String>,
    val resolutions: List<String>,
    val albumId: String?,
    val shortDates: Boolean
)

data class MultiFilterState(
    val dates: Set<String>,
    val imgExts: Set<String>,
    val vidRes: Set<String>,
    val albumId: String?
)
