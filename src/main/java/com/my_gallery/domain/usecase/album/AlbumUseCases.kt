package com.my_gallery.domain.usecase.album

import javax.inject.Inject

data class AlbumUseCases @Inject constructor(
    val saveMediaToAlbum: SaveMediaToAlbumUseCase,
    val loadAlbums: LoadAlbumsUseCase,
    val createAlbum: CreateAlbumUseCase,
    val prepareTempAlbum: PrepareTempAlbumUseCase
)
