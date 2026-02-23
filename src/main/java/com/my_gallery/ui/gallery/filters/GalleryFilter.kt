package com.my_gallery.ui.gallery.filters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

interface GalleryFilter {
    val title: String

    @Composable
    fun getOptions(): State<List<String>>

    @Composable
    fun getSelectedOptions(): State<Set<String>>

    fun onOptionSelected(option: String)
}
