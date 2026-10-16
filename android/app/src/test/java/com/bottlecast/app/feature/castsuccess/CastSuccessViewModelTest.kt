package com.bottlecast.app.feature.castsuccess

import androidx.lifecycle.SavedStateHandle
import com.bottlecast.app.common.messages.domain.CastStatus
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/** Unit tests for [CastSuccessViewModel]. */
class CastSuccessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val status = MutableStateFlow(CastStatus.Queued)
    private val casts = mockk<ICastCoordinator> { every { status(any()) } returns status }

    private fun handle(played: Boolean = false) = SavedStateHandle(
        mapOf("messageId" to "bottle00000000000013", "queued" to true, "sealable" to false, "played" to played),
    )

    @Test
    fun `a queued cast turns to drifting once the server has it`() = runTest {
        // given
        val viewModel = CastSuccessViewModel(casts, handle())
        val queued = viewModel.state.value.queued

        // when
        status.value = CastStatus.Confirmed

        // then
        Assert.assertTrue(queued)
        Assert.assertFalse(viewModel.state.value.queued)
        Assert.assertFalse(viewModel.state.value.sealable)
    }

    @Test
    fun `once played, a recreated screen doesn't play the cast again`() = runTest {
        // given
        val savedState = handle()
        CastSuccessViewModel(casts, savedState).onPlayed()

        // when
        val recreated = CastSuccessViewModel(casts, savedState)

        // then
        Assert.assertTrue(recreated.state.value.played)
    }
}
