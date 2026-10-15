package com.bottlecast.app.feature.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.bottlecast.app.testing.MainDispatcherRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for [LicencesViewModel], on the list and licence texts the build generates. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], application = Application::class)
class LicencesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = LicencesViewModel(ApplicationProvider.getApplicationContext(), mainDispatcherRule.testDispatcher)

    @Test
    fun `every library is listed by name, with its licence and a page to open`() {
        // when
        val showing = viewModel().state.value as LicencesUiState.Showing

        // then
        val names = showing.libraries.map { it.name.lowercase() }
        Assert.assertTrue(showing.libraries.size > 50)
        Assert.assertEquals(names.sorted(), names)
        val activity = showing.libraries.first { it.id == "androidx.activity:activity" }
        Assert.assertEquals(listOf("Apache License, Version 2.0"), activity.licences)
        Assert.assertNotNull(activity.url)
    }

    @Test
    fun `a bundled font's licence opens in full, and closes`() {
        // given
        val viewModel = viewModel()

        // when
        viewModel.onOpenBundled(BundledAsset.RobotoFlex)
        val open = (viewModel.state.value as LicencesUiState.Showing).openText
        viewModel.onCloseBundled()

        // then
        Assert.assertEquals(BundledAsset.RobotoFlex, open?.asset)
        Assert.assertTrue(open?.text.orEmpty().contains("SIL OPEN FONT LICENSE"))
        Assert.assertNull((viewModel.state.value as LicencesUiState.Showing).openText)
    }

    @Test
    fun `every bundled licence ships with the app`() {
        // given
        val assets = ApplicationProvider.getApplicationContext<Application>().assets

        // when
        val missing = BundledAsset.entries.filter { asset -> runCatching { assets.open(asset.licenceFile).close() }.isFailure }

        // then
        Assert.assertEquals(emptyList<BundledAsset>(), missing)
    }
}
