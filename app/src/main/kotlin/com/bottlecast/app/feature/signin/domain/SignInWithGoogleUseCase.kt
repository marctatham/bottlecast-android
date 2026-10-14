package com.bottlecast.app.feature.signin.domain

import com.bottlecast.app.common.authentication.domain.GoogleLinkResult
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.sea.domain.HandoverResult
import com.bottlecast.app.common.sea.domain.ISeaActions
import com.bottlecast.app.common.sea.domain.ISeaRepository
import javax.inject.Inject

/** How signing in went, for analytics' `signed_in`. */
enum class SignInOutcome(val analyticsValue: String) {
    /** The guest gained Google and kept its uid, finds and all. */
    Linked("linked"),

    /** Nobody was signed in. */
    SignedIn("signed_in"),

    /** The Google account already existed, and today's find stayed with the guest, or there was none. */
    Switched("switched"),

    /** The Google account already existed, and today's find came with the guest (spec §3.2). */
    HandedOver("handed_over"),
}

/**
 * Signs in with Google (spec §3.2). A guest is linked, keeping its uid. If that Google account
 * already exists, the guest first writes what it was doing and arms a handoff of today's find, then
 * switches; the Google account then claims the find, unless it has one of its own today. Only the
 * sign-in itself can fail it: the handover is a bonus, and its failures are logged where they happen.
 */
class SignInWithGoogleUseCase @Inject constructor(
    private val authentication: IAuthenticationRepository,
    private val sea: ISeaRepository,
    private val seaActions: ISeaActions,
) {

    suspend operator fun invoke(idToken: String): SignInOutcome {
        when (authentication.linkGoogle(idToken)) {
            GoogleLinkResult.Linked -> return SignInOutcome.Linked
            GoogleLinkResult.SignedIn -> return SignInOutcome.SignedIn
            GoogleLinkResult.AccountExists -> Unit
        }
        // Firestore keeps each account's writes apart, so the guest's go first, while it's the guest
        seaActions.settle()
        val handover = sea.prepareHandover()
        authentication.switchToGoogle(idToken)
        if (handover == null) return SignInOutcome.Switched
        return if (sea.claimHandover(handover) == HandoverResult.HandedOver) SignInOutcome.HandedOver else SignInOutcome.Switched
    }
}
