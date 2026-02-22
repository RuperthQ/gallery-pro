package com.my_gallery.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Utility to parse dates from media filenames when MediaStore metadata is corrupt or missing (e.g., 1969/1970).
 */
object MediaDateParser {

    private val datePatterns = listOf(
        // Pattern: 2022-03-20-00-37-08 (User's specific format)
        Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{2})"),
        // Pattern: 20231027_101122
        Pattern.compile("(\\d{4})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})(\\d{2})"),
        // Pattern: 20231027 (Date only)
        Pattern.compile("(\\d{4})(\\d{2})(\\d{2})"),
        // Pattern: 2023-10-27 (Date only)
        Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})"),
        // Pattern: 2023_10_27 (Date only)
        Pattern.compile("(\\d{4})_(\\d{2})_(\\d{2})"),
        // Pattern: IMG-20231027-WA
        Pattern.compile("IMG-(\\d{4})(\\d{2})(\\d{2})-WA"),
        // Pattern: VID-20231027-WA
        Pattern.compile("VID-(\\d{4})(\\d{2})(\\d{2})-WA")
    )

    fun parseDateFromFileName(fileName: String, fallbackTimestamp: Long): Long {
        if (fallbackTimestamp > 946684800000L) return fallbackTimestamp

        for (pattern in datePatterns) {
            val matcher = pattern.matcher(fileName)
            if (matcher.find()) {
                try {
                    val year = matcher.group(1).toInt()
                    val month = matcher.group(2).toInt() - 1 
                    val day = matcher.group(3).toInt()
                    
                    // Intentar extraer hora si el patrón tiene suficientes grupos
                    val hour = if (matcher.groupCount() >= 4) matcher.group(4).toInt() else 12
                    val min = if (matcher.groupCount() >= 5) matcher.group(5).toInt() else 0
                    val sec = if (matcher.groupCount() >= 6) matcher.group(6).toInt() else 0

                    if (year in 1990..2100 && month in 0..11 && day in 1..31) {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                            set(Calendar.MINUTE, min.coerceIn(0, 59))
                            set(Calendar.SECOND, sec.coerceIn(0, 59))
                            set(Calendar.MILLISECOND, 0)
                        }
                        return calendar.timeInMillis
                    }
                } catch (e: Exception) {
                    // Continuar
                }
            }
        }
        return fallbackTimestamp
    }
}
