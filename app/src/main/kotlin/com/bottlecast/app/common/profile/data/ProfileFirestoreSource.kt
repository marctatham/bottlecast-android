package com.bottlecast.app.common.profile.data

import com.bottlecast.app.common.profile.data.mapper.ProfileMapper
import com.bottlecast.app.common.profile.data.model.ProfileFields
import com.bottlecast.app.common.profile.domain.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

class ProfileFirestoreSource @Inject constructor(
    firestore: FirebaseFirestore,
    private val mapper: ProfileMapper,
) : IProfileDataSource {

    private val users = firestore.collection(ProfileFields.COLLECTION)

    // a listener that ends in an error reads as an empty profile, so Preview still asks for the Terms
    override fun observeProfile(uid: String): Flow<UserProfile> = callbackFlow {
        val registration = users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) Timber.w(error, "[observeProfile] Stopped listening to the profile")
            trySend(mapper.mapFrom(snapshot?.takeIf { it.exists() }?.data))
        }
        awaitClose { registration.remove() }
    }

    override fun savePenName(uid: String, penName: String) {
        users.document(uid)
            .set(mapOf(ProfileFields.PEN_NAME to penName), SetOptions.merge())
            .addOnFailureListener { Timber.e(it, "[savePenName] The server turned the pen name down") }
    }
}
