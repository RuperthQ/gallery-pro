package com.my_gallery.domain.usecase.album

import android.os.Environment
import com.my_gallery.domain.model.AlbumItem
import java.io.File
import javax.inject.Inject

class PrepareTempAlbumUseCase @Inject constructor() {
    operator fun invoke(name: String): AlbumItem {
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val albumPath = File(dcim, "Gallery_Pro/$name").absolutePath.lowercase()
        return AlbumItem(
            id = albumPath.hashCode().toString(),
            name = name,
            thumbnail = "",
            count = 0
        )
    }
}
