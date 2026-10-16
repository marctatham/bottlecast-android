package com.bottlecast.app.feature.penname

import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.testing.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

/** Unit tests for [PenNameViewModel]: typing, suggestions and saving a pen name. */
class PenNameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val profile = mockk<IProfileRepository> {
        every { this@mockk.profile } returns flowOf(UserProfile())
        coEvery { savePenName(any()) } just Runs
    }

    @Test
    fun `a pen name of 3 to 20 characters saves, trimmed`() = runTest {
        // given
        val viewModel = PenNameViewModel(profile, Random(1))

        // when
        viewModel.onNameChange("  Driftwood ")
        viewModel.onSave()

        // then
        coVerify { profile.savePenName("Driftwood") }
        Assert.assertEquals(PenNameUiState.Saved("Driftwood", suggested = false), viewModel.state.value)
    }

    @Test
    fun `a name too short shows the error, and saves nothing`() = runTest {
        // given
        val viewModel = PenNameViewModel(profile, Random(1))

        // when
        viewModel.onNameChange("Al")
        viewModel.onSave()

        // then
        Assert.assertEquals(PenNameUiState.Editing("Al", showError = true), viewModel.state.value)
        coVerify(exactly = 0) { profile.savePenName(any()) }
    }

    @Test
    fun `typing stops at 20 characters, and clears the error`() = runTest {
        // given
        val viewModel = PenNameViewModel(profile, Random(1))
        viewModel.onSave()

        // when
        viewModel.onNameChange("A".repeat(25))

        // then
        Assert.assertEquals(PenNameUiState.Editing("A".repeat(20), showError = false), viewModel.state.value)
    }

    // an emoji is two UTF-16 units, as the rules count it, and half of one isn't a character
    @Test
    fun `an emoji that doesn't fit at the end is left out whole`() = runTest {
        // given
        val viewModel = PenNameViewModel(profile, Random(1))

        // when
        viewModel.onNameChange("A".repeat(19) + "\uD83C\uDF0A")

        // then
        Assert.assertEquals("A".repeat(19), (viewModel.state.value as PenNameUiState.Editing).name)
    }

    @Test
    fun `surprise me suggests a different name each time, and a saved suggestion says so`() = runTest {
        // given
        val viewModel = PenNameViewModel(profile, Random(1))
        val suggestions = listOf("Driftwood", "Quiet Otter")

        // when
        viewModel.onSurpriseMe(suggestions)
        val first = (viewModel.state.value as PenNameUiState.Editing).name
        viewModel.onSurpriseMe(suggestions)
        val second = (viewModel.state.value as PenNameUiState.Editing).name
        viewModel.onSave()

        // then
        Assert.assertNotEquals(first, second)
        Assert.assertEquals(PenNameUiState.Saved(second, suggested = true), viewModel.state.value)
    }

    @Test
    fun `the pen name there is now starts the field`() = runTest {
        // given
        every { profile.profile } returns flowOf(UserProfile(penName = "Tidewise"))

        // when
        val viewModel = PenNameViewModel(profile, Random(1))

        // then
        Assert.assertEquals(PenNameUiState.Editing("Tidewise"), viewModel.state.value)
    }
}
