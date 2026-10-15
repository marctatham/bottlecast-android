package com.bottlecast.app.common.account.domain

import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.IGoogleCredentials
import com.bottlecast.app.common.sea.domain.ISeaActions
import javax.inject.Inject

/**
 * Signs out, back to guest mode on this phone (spec §3.17). A silent guest is made again when one is
 * next needed. Firestore's write queue belongs to the signed-in account, so a throw back or report
 * still waiting is written first.
 */
class SignOutUseCase @Inject constructor(
    private val authenticationRepository: IAuthenticationRepository,
    private val seaActions: ISeaActions,
    private val credentials: IGoogleCredentials,
) {
    suspend operator fun invoke() {
        seaActions.settle()
        authenticationRepository.signOut()
        credentials.forgetAccount()
    }
}
