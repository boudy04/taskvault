package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.data.source.local.PendingOpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Refactor Step 3 gate: the mutation→op-type policy pinned case by case in one place. */
class PendingOpClassifierTest {

    @Test
    fun `create queues only for team rows`() {
        assertEquals(PendingOpType.CREATE, PendingOpClassifier.forCreate(isPersonal = false))
        assertNull(PendingOpClassifier.forCreate(isPersonal = true))
    }

    @Test
    fun `update queues only for team rows`() {
        assertEquals(PendingOpType.UPDATE, PendingOpClassifier.forUpdate(isPersonal = false))
        assertNull(PendingOpClassifier.forUpdate(isPersonal = true))
    }

    @Test
    fun `member with server-known row sends status only`() {
        assertEquals(
            PendingOpType.STATUS,
            PendingOpClassifier.forStatusChange(isPersonal = false, isMember = true, hasServerId = true),
        )
    }

    @Test
    fun `member without server row falls back to update`() {
        assertEquals(
            PendingOpType.UPDATE,
            PendingOpClassifier.forStatusChange(isPersonal = false, isMember = true, hasServerId = false),
        )
    }

    @Test
    fun `admin always sends full update`() {
        assertEquals(
            PendingOpType.UPDATE,
            PendingOpClassifier.forStatusChange(isPersonal = false, isMember = false, hasServerId = true),
        )
        assertEquals(
            PendingOpType.UPDATE,
            PendingOpClassifier.forStatusChange(isPersonal = false, isMember = false, hasServerId = false),
        )
    }

    @Test
    fun `personal rows never queue on status change`() {
        assertNull(
            PendingOpClassifier.forStatusChange(isPersonal = true, isMember = true, hasServerId = true),
        )
        assertNull(
            PendingOpClassifier.forStatusChange(isPersonal = true, isMember = false, hasServerId = false),
        )
    }

    @Test
    fun `delete queues only for known team rows`() {
        assertEquals(
            PendingOpType.DELETE,
            PendingOpClassifier.forDelete(isPersonal = false, hasServerId = true),
        )
        assertNull(PendingOpClassifier.forDelete(isPersonal = false, hasServerId = false))
        assertNull(PendingOpClassifier.forDelete(isPersonal = true, hasServerId = true))
    }
}
