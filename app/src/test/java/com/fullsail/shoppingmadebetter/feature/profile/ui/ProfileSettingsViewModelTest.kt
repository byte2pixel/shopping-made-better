package com.fullsail.shoppingmadebetter.feature.profile.ui

import com.fullsail.shoppingmadebetter.feature.profile.domain.GetAutoAdjustEnabledUseCase
import com.fullsail.shoppingmadebetter.feature.profile.domain.SetAutoAdjustEnabledUseCase
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Unit tests for [ProfileSettingsViewModel] with hand-written fakes. */
class ProfileSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeGetAutoAdjustEnabledUseCase(
        private val output: GetAutoAdjustEnabledUseCase.Output =
            GetAutoAdjustEnabledUseCase.Output.Success(enabled = true),
    ) : GetAutoAdjustEnabledUseCase {
        override suspend fun execute(input: Unit) = output
    }

    private class FakeSetAutoAdjustEnabledUseCase(
        private val output: SetAutoAdjustEnabledUseCase.Output = SetAutoAdjustEnabledUseCase.Output.Success,
    ) : SetAutoAdjustEnabledUseCase {
        var lastInput: SetAutoAdjustEnabledUseCase.Input? = null
        override suspend fun execute(input: SetAutoAdjustEnabledUseCase.Input): SetAutoAdjustEnabledUseCase.Output {
            lastInput = input
            return output
        }
    }

    @Test
    fun `init loads the flag`() = runTest {
        val viewModel = ProfileSettingsViewModel(
            FakeGetAutoAdjustEnabledUseCase(GetAutoAdjustEnabledUseCase.Output.Success(enabled = false)),
            FakeSetAutoAdjustEnabledUseCase(),
        )

        assertEquals(false, viewModel.autoAdjustEnabled.value)
    }

    @Test
    fun `a failed load leaves the flag unknown`() = runTest {
        val viewModel = ProfileSettingsViewModel(
            FakeGetAutoAdjustEnabledUseCase(GetAutoAdjustEnabledUseCase.Output.Failure(IOException("boom"))),
            FakeSetAutoAdjustEnabledUseCase(),
        )

        assertNull(viewModel.autoAdjustEnabled.value)
    }

    @Test
    fun `onAutoAdjustToggled saves the new value and keeps it`() = runTest {
        val set = FakeSetAutoAdjustEnabledUseCase()
        val viewModel = ProfileSettingsViewModel(FakeGetAutoAdjustEnabledUseCase(), set)

        viewModel.onAutoAdjustToggled(false)

        assertEquals(SetAutoAdjustEnabledUseCase.Input(enabled = false), set.lastInput)
        assertEquals(false, viewModel.autoAdjustEnabled.value)
    }

    @Test
    fun `onAutoAdjustToggled reverts and emits an event when the save fails`() = runTest {
        val set = FakeSetAutoAdjustEnabledUseCase(
            SetAutoAdjustEnabledUseCase.Output.Failure(IOException("boom"))
        )
        val viewModel = ProfileSettingsViewModel(FakeGetAutoAdjustEnabledUseCase(), set)

        viewModel.onAutoAdjustToggled(false)

        assertEquals(true, viewModel.autoAdjustEnabled.value)
        assertEquals(ProfileSettingsEvent.AutoAdjustUpdateFailed, viewModel.events.first())
    }
}
