package dev.boudy04.taskvault.util

import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DueDatesTest {

    @Test
    fun `format null returns null`() {
        assertNull(DueDates.format(null))
    }

    @Test
    fun `format renders date and 24h time separated by middot`() {
        val iso = "2026-08-24T09:05:00Z"
        val local = DueDates.toLocalDateTime(iso)
        val datePart = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d").format(local)
        val timePart = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(local)
        assertEquals("$datePart · $timePart", DueDates.format(iso))
        assertTrue(DueDates.format(iso)!!.contains(" · "))
    }

    @Test
    fun `iso round trip preserves wall clock time`() {
        val original = LocalDateTime.of(2026, 8, 24, 14, 30)
        val back = DueDates.toLocalDateTime(DueDates.toIso(original))
        assertEquals(original, back.truncatedTo(java.time.temporal.ChronoUnit.MINUTES))
    }

    @Test
    fun `nextFullHour truncates and steps one hour`() {
        val now = LocalDateTime.of(2026, 8, 23, 14, 37)
        assertEquals(LocalDateTime.of(2026, 8, 23, 15, 0), DueDates.nextFullHour(now))
    }

    @Test
    fun `stored iso parses as UTC instant`() {
        val iso = "2026-08-24T09:00:00Z"
        assertTrue(DueDates.toIso(DueDates.toLocalDateTime(iso)).endsWith("Z"))
    }
}
