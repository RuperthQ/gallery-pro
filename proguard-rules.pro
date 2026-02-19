# Preservar modelos de datos y entidades de Room
-keep @androidx.annotation.Keep class *
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class androidx.room.PagingSource
-keep class * extends androidx.room.PagingSource

# Hilt
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends android.app.Application

# Paging 3
-keep class androidx.paging.** { *; }

# Mantener nuestros modelos específicos
-keep class com.my_gallery.domain.model.** { *; }
-keep class com.my_gallery.data.local.entity.** { *; }
-keep class com.my_gallery.ui.gallery.GalleryUiModel { *; }
-keep class com.my_gallery.ui.gallery.GalleryUiModel$* { *; }