package com.bottlecast.app.common.messages.data.model

import androidx.annotation.Keep

// Firestore serialises this via reflection (its getters become the document's fields),
// so R8 must not rename or strip them in minified release builds
@Keep
data class MessageFirestoreDataModel(
    val messageContent: String,
    val userId: String
)
