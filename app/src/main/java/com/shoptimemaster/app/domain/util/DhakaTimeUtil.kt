package com.shoptimemaster.app.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DhakaTimeUtil {
    val DHAKA_ZONE: ZoneId = ZoneId.of("Asia/Dhaka")
    private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') sb.append(BN_DIGITS[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }

    fun formatBdt(amount: Double, bengaliDigits: Boolean): String {
        val rounded = kotlin.math.round(amount).toLong()
        val formatted = "৳%,d".format(Locale.US, rounded)
        return if (bengaliDigits) toBengaliDigits(formatted) else formatted
    }

    fun localDateFromEpoch(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(DHAKA_ZONE).toLocalDate()

    fun formatTime(epochMs: Long, bengaliDigits: Boolean): String {
        val text = Instant.ofEpochMilli(epochMs)
            .atZone(DHAKA_ZONE)
            .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US))
        return if (bengaliDigits) toBengaliDigits(text) else text
    }

    fun splitIntervalByDhakaDate(startMs: Long, endMs: Long): Map<LocalDate, Long> {
        if (endMs <= startMs) return emptyMap()
        val result = mutableMapOf<LocalDate, Long>()
        var cursor = startMs
        while (cursor < endMs) {
            val date = localDateFromEpoch(cursor)
            val nextMidnightMs = date.plusDays(1).atStartOfDay(DHAKA_ZONE).toInstant().toEpochMilli()
            val segEnd = minOf(endMs, nextMidnightMs)
            val minutes = (segEnd - cursor) / 60_000L
            result[date] = (result[date] ?: 0L) + minutes
            cursor = segEnd
        }
        return result
    }
}
