package com.bottlecast.app.common.account.data.model

/**
 * `deletions/{uid}`: the tombstone an account leaves as it's deleted. firestore.rules lets the account
 * delete its records while it exists, and stops it finding or casting. It's never removed.
 */
object DeletionFields {
    const val COLLECTION = "deletions"

    const val CREATED_AT = "createdAt"
}
