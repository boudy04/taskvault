package dev.boudy04.taskvault.data

import dev.boudy04.taskvault.data.source.local.PendingOpType

/**
 * Single seam for the "which pending op does this mutation produce?" policy (refactor Step 3).
 * Pure and side-effect free: the repository reads the session and passes [isMember] in, so the
 * rules below are trivially unit-testable. Returns null when the mutation must NOT queue.
 */
object PendingOpClassifier {

    /** Personal rows are LOCAL-ONLY: never queued, never pushed. */
    fun forCreate(isPersonal: Boolean): PendingOpType? =
        if (isPersonal) null else PendingOpType.CREATE

    /**
     * Team edits enqueue a full UPDATE. The server discloses role limits on drain
     * (403 → op dropped + view-only signal), so no role branching happens here.
     */
    fun forUpdate(isPersonal: Boolean): PendingOpType? =
        if (isPersonal) null else PendingOpType.UPDATE

    /**
     * Members may only send {"status": ...} and only for rows the server already knows;
     * anything else falls back to a full UPDATE (which the server rejects for members).
     */
    fun forStatusChange(
        isPersonal: Boolean,
        isMember: Boolean,
        hasServerId: Boolean,
    ): PendingOpType? = when {
        isPersonal -> null
        isMember && hasServerId -> PendingOpType.STATUS
        else -> PendingOpType.UPDATE
    }

    /** A tombstone only matters for rows the server knows about. */
    fun forDelete(isPersonal: Boolean, hasServerId: Boolean): PendingOpType? =
        if (isPersonal || !hasServerId) null else PendingOpType.DELETE
}
