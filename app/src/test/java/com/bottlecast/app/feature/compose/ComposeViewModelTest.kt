package com.bottlecast.app.feature.compose

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.bottlecast.app.common.messages.domain.Draft
import com.bottlecast.app.common.messages.domain.ICastCoordinator
import com.bottlecast.app.common.messages.domain.IDraftRepository
import com.bottlecast.app.common.messages.domain.ILanguageDetector
import com.bottlecast.app.feature.compose.domain.ComposeHelper
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** Unit tests for [ComposeViewModel]: the draft, the helper, and closing with words written. */
class ComposeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-10-14T09:00:00Z"))
    private val draft = MutableStateFlow<Draft?>(null)
    private val drafts = mockk<IDraftRepository>(relaxed = true) { every { this@mockk.draft } returns this@ComposeViewModelTest.draft }
    private val casts = mockk<ICastCoordinator> { every { castsLeftToday } returns MutableStateFlow(5) }
    private val languages = mockk<ILanguageDetector> { coEvery { detect(any()) } returns "en" }

    private fun viewModel(justSignedIn: Boolean = false) =
        ComposeViewModel(drafts, casts, languages, clock, SavedStateHandle(mapOf("justSignedIn" to justSignedIn)))

    private fun ComposeViewModel.write(text: String) {
        editor.setTextAndPlaceCursorAtEnd(text)
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun `a saved draft comes back, and says so once`() = runTest {
        // given
        coEvery { drafts.load() } returns Draft("bottle00000000000001", "Leave the porch light on")

        // when
        val viewModel = viewModel()

        // then
        Assert.assertEquals("Leave the porch light on", viewModel.editor.text.toString())
        Assert.assertEquals(ComposeEffect.RestoredDraft, viewModel.effects.first())
    }

    @Test
    fun `a cast refused while Compose is open comes back into the empty editor, and says so`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()

        // when
        draft.value = Draft("bottle00000000000001", "Leave the porch light on")

        // then
        Assert.assertEquals("Leave the porch light on", viewModel.editor.text.toString())
        Assert.assertEquals(ComposeEffect.RestoredDraft, viewModel.effects.first())
    }

    @Test
    fun `words being written aren't replaced by a draft`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()
        viewModel.write("Something new")

        // when
        draft.value = Draft("bottle00000000000001", "Leave the porch light on")

        // then
        Assert.assertEquals("Something new", viewModel.editor.text.toString())
    }

    @Test
    fun `the editor opens even when the saved draft can't be read`() = runTest {
        // given
        coEvery { drafts.load() } throws IllegalStateException("disk")

        // when
        val viewModel = viewModel()

        // then
        Assert.assertTrue(viewModel.state.value is ComposeUiState.Writing)
    }

    @Test
    fun `just signed in, it offers a pen name`() = runTest {
        // given
        coEvery { drafts.load() } returns null

        // when
        val viewModel = viewModel(justSignedIn = true)

        // then
        Assert.assertEquals(ComposeEffect.SignedIn, viewModel.effects.first())
    }

    @Test
    fun `writing goes to the draft, and enough of it shows the language`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()

        // when
        viewModel.write("If you're reading this, drink some water.")
        advanceTimeBy(1_000)

        // then
        verify { drafts.update("If you're reading this, drink some water.") }
        Assert.assertEquals(ComposeHelper.Language("en"), (viewModel.state.value as ComposeUiState.Writing).helper)
    }

    @Test
    fun `Preview tapped too soon turns the minimum red, with a buzz`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()
        viewModel.write("Hello sea")

        // when
        viewModel.onPreviewTooShort()

        // then
        Assert.assertEquals(ComposeHelper.Minimum(asError = true), (viewModel.state.value as ComposeUiState.Writing).helper)
        Assert.assertEquals(ComposeEffect.Rejected, viewModel.effects.first())
    }

    @Test
    fun `Preview saves the draft first`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()

        // when
        viewModel.onPreview()

        // then
        Assert.assertEquals(ComposeEffect.OpenPreview, viewModel.effects.first())
        coVerifyOrder { drafts.flush() }
    }

    @Test
    fun `closing with words written asks first, and tossing clears them`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()
        viewModel.write("Leave the porch light on")

        // when
        viewModel.onClose()
        val asking = (viewModel.state.value as ComposeUiState.Writing).discarding
        viewModel.onToss()

        // then
        Assert.assertTrue(asking)
        Assert.assertEquals("", viewModel.editor.text.toString())
        verify { drafts.clear() }
        Assert.assertEquals(ComposeEffect.Close, viewModel.effects.first())
    }

    @Test
    fun `closing with nothing written just closes`() = runTest {
        // given
        coEvery { drafts.load() } returns null
        val viewModel = viewModel()

        // when
        viewModel.onClose()

        // then
        Assert.assertEquals(ComposeEffect.Close, viewModel.effects.first())
    }
}
