package com.bottlecast.app.common.chest.data.model

/**
 * claims/{messageId}: the keeper's claim on a bottle they kept (spec §1.7). Only the keeper reads it,
 * so the writer never learns who kept their bottle.
 */
object ClaimFields {
    const val COLLECTION = "claims"

    const val MESSAGE_ID = "messageId"
    const val KEEPER_UID = "keeperUid"
    const val STATUS = "status"
    const val KEPT_AT = "keptAt"

    /** Kept in the chest, not on-chain: sealing (slice 6) moves a claim on from here. */
    const val STATUS_PENDING_WALLET = "pending_wallet"
}
