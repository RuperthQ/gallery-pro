package com.my_gallery.ui.gallery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.my_gallery.ui.gallery.GalleryViewModel
import com.my_gallery.ui.gallery.filters.FilterOrchestrator
import com.my_gallery.ui.theme.GalleryDesign
import com.my_gallery.ui.theme.GalleryDesign.premiumBorder

@Composable
fun FilterRow(viewModel: GalleryViewModel) {
    val orchestrator = remember(viewModel) { FilterOrchestrator(viewModel) }
    val filters = orchestrator.getFilters()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GalleryDesign.PaddingSmall)
    ) {
        filters.forEach { filter ->
            val options by filter.getOptions()
            val selectedOption by filter.getSelectedOption()

            if (options.isNotEmpty()) {
                FilterSection(
                    label = filter.title,
                    options = options,
                    selectedOption = selectedOption ?: "Todas",
                    onOptionSelected = { filter.onOptionSelected(it) }
                )
            }
        }
    }
}

@Composable
fun FilterSection(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GalleryDesign.PaddingTiny)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GalleryDesign.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(GalleryDesign.PaddingSmall))
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = GalleryDesign.PaddingLarge),
            horizontalArrangement = Arrangement.spacedBy(GalleryDesign.PaddingSmall),
            modifier = Modifier.padding(vertical = GalleryDesign.PaddingSmall)
        ) {
            items(options) { option ->
                GalleryFilterChip(
                    label = option,
                    isSelected = (option == selectedOption) || (option == "Todas" && selectedOption == null) || (option == "Todos" && (selectedOption == null || selectedOption == "Todos")), 
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
fun GalleryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(GalleryDesign.ButtonHeight)
            .premiumBorder(
                shape = GalleryDesign.FilterShape,
                width = if (isSelected) GalleryDesign.BorderWidthBold else GalleryDesign.BorderWidthThin,
                alpha = if (isSelected) 1f else 0.3f
            )
            .clip(GalleryDesign.FilterShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = GalleryDesign.PaddingMedium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
