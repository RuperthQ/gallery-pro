package com.my_gallery.ui.gallery.utils

import java.util.concurrent.TimeUnit

object FormatUtils {
    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private val dateFormatter = java.text.SimpleDateFormat("dd 'de' MMMM 'de' yyyy", java.util.Locale("es", "ES"))
    
    fun formatDate(timestamp: Long): String {
        return dateFormatter.format(java.util.Date(timestamp)).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(java.util.Locale.ROOT) else char.toString() }
    }

    fun formatPeriodLabel(date: java.util.Date, isShort: Boolean): String {
        val cal = java.util.Calendar.getInstance().apply { time = date }
        val month = cal.get(java.util.Calendar.MONTH)
        val year = cal.get(java.util.Calendar.YEAR)
        val monthNameRaw = java.text.DateFormatSymbols(java.util.Locale("es", "ES")).months[month]
        val monthName = if (isShort) {
            monthNameRaw.take(3).replaceFirstChar { it.uppercase() }
        } else {
            monthNameRaw.replaceFirstChar { it.uppercase() }
        }
        return "$monthName $year"
    }

    fun parseDateSafely(date: String?, isShort: Boolean): java.util.Date? {
        if (date.isNullOrEmpty()) return null
        return try {
            val formatStr = if (isShort) "MMM yyyy" else "MMMM yyyy"
            val format = java.text.SimpleDateFormat(formatStr, java.util.Locale("es", "ES"))
            format.parse(date)
        } catch (e: Exception) {
            null
        }
    }
}
