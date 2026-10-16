package com.bottlecast.app.feature.settings

import android.content.Context
import com.bottlecast.app.common.account.domain.DeleteAccountResult
import com.bottlecast.app.common.account.domain.DeleteAccountUseCase
import com.bottlecast.app.common.account.domain.SignOutUseCase
import com.bottlecast.app.common.authentication.domain.AuthProviderType
import com.bottlecast.app.common.authentication.domain.IAuthenticationRepository
import com.bottlecast.app.common.authentication.domain.UserEntity
import com.bottlecast.app.common.profile.domain.IProfileRepository
import com.bottlecast.app.common.profile.domain.UserProfile
import com.bottlecast.app.common.reminders.domain.ReminderSettings
import com.bottlecast.app.common.sea.domain.BlockedWriter
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.settings.domain.AppSettings
import com.bottlecast.app.common.settings.domain.ISettingsStore
import com.bottlecast.app.common.settings.domain.IThemeApplier
import com.bottlecast.app.common.settings.domain.ThemeChoice
import com.bottlecast.app.testing.FakeNotificationAccess
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MemoryReminderStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

/** Unit tests for [SettingsViewModel]. */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = MutableStateFlow<UserEntity>(
        UserEntity.AuthenticatedUser(AuthProviderType.Google, "jwt", "gail", isAdmin = false, googleName = "Gail Storm"),
    )
    private val auth = mockk<IAuthenticationRepository> { every { this@mockk.user } returns this@SettingsViewModelTest.user }
    private val profiles = mockk<IProfileRepository> { every { profile } returns flowOf(UserProfile(penName = "Driftwood")) }
    private val blocked = listOf(BlockedWriter("w1", Instant.parse("2026-10-05T10:00:00Z")))
    private val sea = mockk<ISeaRepository>(relaxed = true) { every { blockedWriters } returns flowOf(blocked) }
    private val settings = MutableStateFlow(AppSettings())
    private val settingsStore = mockk<ISettingsStore>(relaxed = true) { every { this@mockk.settings } returns this@SettingsViewModelTest.settings }
    private val themeApplier = mockk<IThemeApplier>(relaxed = true)
    private val reminders = MemoryReminderStore()
    private val access = FakeNotificationAccess()
    private val signOut = mockk<SignOutUseCase>(relaxed = true)
    private val deleteAccount = mockk<DeleteAccountUseCase>()
    private val context = mockk<Context>(relaxed = true)

    private fun viewModel() = SettingsViewModel(
        auth, profiles, sea, settingsStore, themeApplier, reminders, access, signOut, deleteAccount, context,
    )

    private fun SettingsViewModel.showing() = state.value as SettingsUiState.Showing

    @Test
    fun `a Google account sees its name, pen name and blocked writers`() = runTest {
        // when
        val viewModel = viewModel()

        // then
        Assert.assertEquals(SettingsAccount.Google(googleName = "Gail Storm", penName = "Driftwood"), viewModel.showing().account)
        Assert.assertEquals(blocked, viewModel.showing().blockedWriters)
    }

    @Test
    fun `a guest has no account rows and no blocked writers`() = runTest {
        // given
        user.value = UserEntity.AuthenticatedUser(AuthProviderType.Anonymous, "jwt", "guest", isAdmin = false)

        // when
        val viewModel = viewModel()

        // then
        Assert.assertEquals(SettingsAccount.Guest, viewModel.showing().account)
        Assert.assertEquals(emptyList<BlockedWriter>(), viewModel.showing().blockedWriters)
    }

    @Test
    fun `turning reminders on without Android's permission opens the reminder sheet instead`() = runTest {
        // given
        val viewModel = viewModel()

        // when
        val openSheet = viewModel.onRemindersChange(true)

        // then
        Assert.assertTrue(openSheet)
        Assert.assertFalse(reminders.settings.value.enabled)
    }

    @Test
    fun `turning reminders on and off when Android allows them, saying when they'll come`() = runTest {
        // given
        access.allowed = true
        val viewModel = viewModel()

        // when
        val openSheet = viewModel.onRemindersChange(true)
        val on = viewModel.showing()
        viewModel.onRemindersChange(false)

        // then
        Assert.assertFalse(openSheet)
        Assert.assertTrue(on.remindersOn)
        Assert.assertEquals(SettingsNotice.RemindersOn(LocalTime.of(9, 0)), on.notice)
        Assert.assertFalse(reminders.settings.value.enabled)
    }

    @Test
    fun `reminders Android holds back show as notifications being off`() = runTest {
        // given
        reminders.setEnabled(true)
        access.allowed = true
        val viewModel = viewModel()
        val allowed = viewModel.showing().notificationsOff

        // when
        access.allowed = false
        viewModel.onResume()

        // then
        Assert.assertFalse(allowed)
        Assert.assertTrue(viewModel.showing().notificationsOff)
        Assert.assertFalse(viewModel.showing().remindersOn)
    }

    @Test
    fun `a theme is saved and applied at once`() = runTest {
        // given
        val viewModel = viewModel()

        // when
        viewModel.onThemeChange(ThemeChoice.Dawn)

        // then
        verify { settingsStore.setTheme(ThemeChoice.Dawn) }
        verify { themeApplier.apply(ThemeChoice.Dawn) }
    }

    @Test
    fun `signing out says so once it's done`() = runTest {
        // given
        val viewModel = viewModel()

        // when
        viewModel.onSignOut()

        // then
        coVerify { signOut() }
        Assert.assertEquals(SettingsNotice.SignedOut, viewModel.showing().notice)
        Assert.assertNull(viewModel.showing().busy)
    }

    @Test
    fun `each way deleting ends has its notice, and a closed picker has none`() = runTest {
        // given
        val viewModel = viewModel()
        val results = listOf(
            DeleteAccountResult.Deleted to SettingsNotice.Deleted,
            DeleteAccountResult.Offline to SettingsNotice.DeleteOffline,
            DeleteAccountResult.WrongAccount to SettingsNotice.DeleteWrongAccount,
            DeleteAccountResult.Failed to SettingsNotice.DeleteFailed,
            DeleteAccountResult.Cancelled to null,
        )

        // when
        val notices = results.map { (result, _) ->
            coEvery { deleteAccount(any()) } returns result
            viewModel.onDeleteAccount(context)
            viewModel.showing().notice.also { notice -> notice?.let(viewModel::onNoticeShown) }
        }

        // then
        Assert.assertEquals(results.map { it.second }, notices)
    }

    @Test
    fun `the reminder's time and quiet hours reach the store`() = runTest {
        // given
        val viewModel = viewModel()

        // when
        viewModel.onReminderTimeChange(LocalTime.of(7, 0))
        viewModel.onQuietHoursChange(LocalTime.of(23, 0), LocalTime.of(7, 0))

        // then
        Assert.assertEquals(
            ReminderSettings(time = LocalTime.of(7, 0), quietStart = LocalTime.of(23, 0), quietEnd = LocalTime.of(7, 0)),
            reminders.settings.value,
        )
    }
}
