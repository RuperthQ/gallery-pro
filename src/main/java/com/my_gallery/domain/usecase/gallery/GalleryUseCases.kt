package com.my_gallery.domain.usecase.gallery

import javax.inject.Inject

data class GalleryUseCases @Inject constructor(
    val syncGallery: SyncGalleryUseCase,
    val decryptMedia: DecryptMediaUseCase,
    val clearDecryptedCache: ClearDecryptedCacheUseCase,
    val getMediaIdsByGroup: GetMediaIdsByGroupUseCase,
    val getPagedMedia: GetPagedMediaUseCase,
    val getSectionMetadata: GetSectionMetadataUseCase,
    val getAvailableFilters: GetAvailableMediaFiltersUseCase,
    val getMediaRank: GetMediaRankUseCase,
    val updateFiltersDisplay: UpdateFiltersDisplayUseCase,
    val getMediaChanges: GetMediaChangesUseCase,
    val getGalleryMediaFlow: GetGalleryMediaFlowUseCase,
    val getExternalMedia: GetExternalMediaUseCase
)
