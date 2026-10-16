package com.bottlecast.app.common.account.domain

import android.content.Context
import com.bottlecast.app.common.authentication.domain.GoogleCredentialResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import com.bottlecast.app.common.authentication.domain.googleUid
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.sea.domain.ISeaActions
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

/** How deleting the account went. */
sealed interface DeleteAccountResult {
    data object Deleted : DeleteAccountResult

    /** The account picker was closed: nothing changed. */
    data object Cancelled : DeleteAccountResult

    /** It needs a connection: nothing changed. */
    data object Offline : DeleteAccountResult

    /** The picker chose another Google account than this one: nothing changed. */
    data object WrongAccount : DeleteAccountResult

    /** It stopped part way. Running it again picks up where it stopped. */
    data object Failed : DeleteAccountResult
}

/** What the phone keeps about one account, forgotten once it's deleted. */
fun interface IAccountLocalData {
    suspend fun clear()
}

/**
 * Deletes the Google account (spec §3.17, decided 2026-10-15), in this order:
 * 1. check there's a connection;
 * 2. sign in again with the same account, as Firebase needs a recent sign-in to delete one;
 * 3. write any throw back or report still waiting, as this account;
 * 4. erase its records from Firestore, leaving bottles someone else holds or kept with them, anonymised;
 * 5. delete it from Firebase Auth, which signs it out;
 * 6. forget what the phone kept about it.
 */
class DeleteAccountUseCase @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    private val credentials: IGoogleCredentials,
    private val connectivityMonitor: IConnectivityMonitor,
    private val seaActions: ISeaActions,
    private val eraser: IAccountEraser,
    private val localData: IAccountLocalData,
) {
    /** [context] is the activity the account picker shows over. */
    suspend operator fun invoke(context: Context): DeleteAccountResult {
        val uid = authenticationRepository.user.value.googleUid ?: return DeleteAccountResult.Failed
        if (!connectivityMonitor.isOnline.value) return DeleteAccountResult.Offline
        val idToken = when (val picked = credentials.requestIdToken(context)) {
            is GoogleCredentialResult.Token -> picked.idToken
            GoogleCredentialResult.Cancelled -> return DeleteAccountResult.Cancelled
            GoogleCredentialResult.Unavailable, GoogleCredentialResult.Failed -> return DeleteAccountResult.Failed
        }
        return try {
            if (!authenticationRepository.reauthenticateWithGoogle(idToken)) return DeleteAccountResult.WrongAccount
            seaActions.settle()
            eraser.erase(uid)
            authenticationRepository.deleteUser()
            localData.clear()
            credentials.forgetAccount()
            DeleteAccountResult.Deleted
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(exception, "[deleteAccount] Stopped part way through deleting the account")
            DeleteAccountResult.Failed
        }
    }
}
