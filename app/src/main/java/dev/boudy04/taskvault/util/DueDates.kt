package dev.boudy04.taskvault.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Due-date conversions between ISO-8601 UTC strings (storage format) and local wall time.
 * ponytail: system-default zone on purpose; a timezone picker is a spec non-goal.
 */
object DueDates {

    private val dateFormat = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    /** "Sat, Aug 23 · 14:30" in the device zone, or null when unset. */
    fun format(isoUtc: String?): String? {
        val local = toLocalDateTime(isoUtc ?: return null)
        return "${local.format(dateFormat)} · ${local.format(timeFormat)}"
    }

    fun toLocalDateTime(isoUtc: String): LocalDateTime =
        LocalDateTime.ofInstant(Instant.parse(isoUtc), ZoneId.systemDefault())

    fun toIso(localDateTime: LocalDateTime): String =
        localDateTime.atZone(ZoneId.systemDefault()).toInstant().toString()

    /** The default pick time when no due is set yet: the next full hour. */
    fun nextFullHour(now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
        now.plusHours(1).truncatedTo(ChronoUnit.HOURS)

    /** Coarse relative stamp for note timestamps: "3m", "5h", "2d" ago; falls back to empty. */
    fun relative(isoUtc: String?): String {
        val instant = runCatching { Instant.parse(isoUtc ?: return "") }.getOrNull() ?: return ""
        val minutes = ChronoUnit.MINUTES.between(instant, Instant.now())
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 1440 -> "${minutes / 60}h"
            else -> "${minutes / 1440}d"
        }
    }
}
