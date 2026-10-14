package com.bottlecast.app.feature.discover

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bottlecast.app.common.components.bottle.BottleLook
import com.bottlecast.app.common.components.bottle.bottleLookFor
import com.bottlecast.app.common.connectivity.IConnectivityMonitor
import com.bottlecast.app.common.messages.domain.MessageEntity
import com.bottlecast.app.common.sea.domain.CandidateState
import com.bottlecast.app.common.sea.domain.FindResult
import com.bottlecast.app.common.sea.domain.FindStatus
import com.bottlecast.app.common.sea.domain.ISeaRepository
import com.bottlecast.app.common.sea.domain.TodayFind
import com.bottlecast.app.feature.discover.domain.UncorkEffect
import com.bottlecast.app.feature.discover.domain.UncorkMethod
import com.bottlecast.app.testing.MainDispatcherRule
import com.bottlecast.app.testing.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.time.TestTimeSource

class DiscoverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val bottle = MessageEntity(id = "abc", message = "Leave the porch light on.", userId = "writer")
    private val candidate = MutableStateFlow<CandidateState>(CandidateState.Available(bottle))
    private val online = MutableStateFlow(true)
    private var todayRecord: TodayFind? = null
    private val clock = MutableClock(Instant.parse("2026-10-13T09:00:00Z"))
    private val timeSource = TestTimeSource()
    private val savedState = SavedStateHandle()

    private val seaRepository = mockk<ISeaRepository>(relaxed = true) {
        every { candidate } returns this@DiscoverViewModelTest.candidate
        every { observeToday(any()) } answers { flowOf(todayRecord) }
        coEvery { prefetchCandidate() } answers { candidate.value }
        coEvery { findTodaysBottle() } returns FindResult.Found(bottle)
    }
    private val connectivity = mockk<IConnectivityMonitor> { every { isOnline } returns online }

    @Test
    fun `the bottle on the sand is the candidate's own`() = runTest {
        // given
        val viewModel = discover()

        // when
        runCurrent()

        // then
        Assert.assertEquals(ready(look = bottleLookFor("abc")), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `before the candidate is known, a plain bottle waits`() = runTest {
        // given
        candidate.value = CandidateState.Unknown
        val viewModel = discover()

        // when
        runCurrent()

        // then
        Assert.assertEquals(ready(look = BottleLook.Default), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `a calm sea shows before anything is uncorked`() = runTest {
        // given
        candidate.value = CandidateState.SeaCalm
        val viewModel = discover()

        // when
        runCurrent()

        // then
        Assert.assertEquals(DiscoverUiState.SeaCalm, viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `offline, fog rolls in, and it clears once the phone is back online`() = runTest {
        // given
        online.value = false
        val viewModel = discover()
        runCurrent()
        val offline = viewModel.state.value

        // when
        online.value = true
        runCurrent()

        // then
        Assert.assertEquals(DiscoverUiState.Offline, offline)
        Assert.assertTrue(viewModel.state.value is DiscoverUiState.Ready)
        close(viewModel)
    }

    @Test
    fun `when today already has a find, it's the Shore's to show`() = runTest {
        // given
        todayRecord = TodayFind(LocalDate.of(2026, 10, 13), "earlier", bottle, FindStatus.Held)
        val viewModel = discover()

        // when
        runCurrent()

        // then
        Assert.assertEquals(DiscoverUiState.AlreadyFound, viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `shakes loosen the cork without recording anything`() = runTest {
        // given
        val viewModel = discover()
        runCurrent()

        // when
        viewModel.onShake()
        viewModel.onShake()
        runCurrent()

        // then
        Assert.assertEquals(ready(steps = 2), viewModel.state.value)
        Assert.assertEquals(2f / 3, viewModel.ringProgress.value, 0.0001f)
        coVerify(exactly = 0) { seaRepository.findTodaysBottle() }
        close(viewModel)
    }

    @Test
    fun `the pop records today's find once, then reveals it`() = runTest {
        // given
        val viewModel = discover()
        runCurrent()

        // when
        viewModel.onUncork(UncorkMethod.Button)
        viewModel.onUncork(UncorkMethod.Button)
        viewModel.onShake()
        runCurrent()

        // then
        Assert.assertEquals(ready(pop = PopPhase.Revealed("abc")), viewModel.state.value)
        coVerify(exactly = 1) { seaRepository.findTodaysBottle() }
        close(viewModel)
    }

    @Test
    fun `a look at the sea that fails while the find waits doesn't hide the find`() = runTest {
        // given
        coEvery { seaRepository.prefetchCandidate() } coAnswers {
            delay(100)
            throw IllegalStateException("unavailable")
        }
        coEvery { seaRepository.findTodaysBottle() } coAnswers {
            delay(200)
            FindResult.Found(bottle)
        }
        val viewModel = discover()

        // when
        viewModel.onUncork(UncorkMethod.Button)
        advanceTimeBy(300)
        runCurrent()

        // then
        Assert.assertEquals(ready(pop = PopPhase.Revealed("abc")), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `a slow find leaves the scroll half out, then the bottle slips back, and trying again finds it`() = runTest {
        // given
        coEvery { seaRepository.findTodaysBottle() } coAnswers {
            delay(10_000)
            FindResult.Found(bottle)
        }
        val viewModel = discover()
        runCurrent()

        // when
        viewModel.onUncork(UncorkMethod.Button)
        runCurrent()
        val recording = viewModel.state.value
        advanceTimeBy(601)
        val late = viewModel.state.value
        advanceTimeBy(4_000)
        val failed = viewModel.state.value
        coEvery { seaRepository.findTodaysBottle() } returns FindResult.Found(bottle)
        viewModel.onTryAgain()
        runCurrent()

        // then
        Assert.assertEquals(ready(pop = PopPhase.Recording), recording)
        Assert.assertEquals(ready(pop = PopPhase.Late), late)
        Assert.assertEquals(DiscoverUiState.Failed, failed)
        Assert.assertEquals(ready(pop = PopPhase.Revealed("abc")), viewModel.state.value)
        close(viewModel)
    }

    @Test
    fun `a find that comes back without a bottle says why`() = runTest {
        for ((result, expected) in listOf(
            FindResult.AlreadyFound("earlier") to DiscoverUiState.AlreadyFound,
            FindResult.SeaCalm to DiscoverUiState.SeaCalm,
            FindResult.Offline to DiscoverUiState.Offline,
            FindResult.Failed to DiscoverUiState.Failed,
        )) {
            // given
            coEvery { seaRepository.findTodaysBottle() } returns result
            val viewModel = discover()
            runCurrent()

            // when
            viewModel.onUncork(UncorkMethod.Button)
            runCurrent()

            // then
            Assert.assertEquals(expected, viewModel.state.value)
            close(viewModel)
        }
    }

    @Test
    fun `the cork's progress survives the screen being recreated`() = runTest {
        // given
        val first = discover()
        runCurrent()
        first.onShake()
        first.onShake()
        close(first)

        // when
        val second = discover()
        runCurrent()

        // then
        Assert.assertEquals(ready(steps = 2), second.state.value)
        close(second)
    }

    @Test
    fun `each shake and the pop reach the screen, for haptics and the wobble`() = runTest {
        // given
        val viewModel = discover()
        val effects = mutableListOf<UncorkEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effects.toList(effects) }
        runCurrent()

        // when
        viewModel.onShake()
        viewModel.onShake()
        viewModel.onShake()
        runCurrent()

        // then
        Assert.assertEquals(
            listOf(UncorkEffect.Step(1), UncorkEffect.Step(2), UncorkEffect.Pop(UncorkMethod.Shake, afterStep = true)),
            effects,
        )
        close(viewModel)
    }

    @Test
    fun `holding the bottle fills the ring as frames pass`() = runTest {
        // given
        val viewModel = discover()
        runCurrent()

        // when
        viewModel.onBottlePressed()
        val moving = (viewModel.state.value as DiscoverUiState.Ready).moving
        timeSource += kotlin.time.Duration.parse("600ms")
        viewModel.onFrame()

        // then
        Assert.assertTrue(moving)
        Assert.assertEquals(0.5f, viewModel.ringProgress.value, 0.0001f)
        close(viewModel)
    }

    private fun discover() = DiscoverViewModel(seaRepository, connectivity, clock, timeSource, savedState)

    private fun ready(
        look: BottleLook = bottleLookFor("abc"),
        steps: Int = 0,
        pop: PopPhase = PopPhase.NotPopped,
    ) = DiscoverUiState.Ready(look = look, steps = steps, moving = false, pop = pop)

    private fun close(viewModel: DiscoverViewModel) = viewModel.viewModelScope.cancel()
}
