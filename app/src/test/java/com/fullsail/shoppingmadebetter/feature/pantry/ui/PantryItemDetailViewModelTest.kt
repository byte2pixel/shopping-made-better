package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [PantryItemDetailViewModel]. A hand-written fake
 * [GetInventoryItemUseCase] drives each arm of the sealed output; the
 * [MainDispatcherRule] lets `viewModelScope` work run on the JVM.
 */
class PantryItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Fake use case: returns [output] and records the id it was asked for. */
    private class FakeGetInventoryItemUseCase(
        var output: GetInventoryItemUseCase.Output,
    ) : GetInventoryItemUseCase {
        var requestedId: String? = null
        override suspend fun execute(input: String): GetInventoryItemUseCase.Output {
            requestedId = input
            return output
        }
    }

    /** Fake threshold use case: records the input and returns [output]. */
    private class FakeUpdateThresholdUseCase(
        var output: UpdateInventoryLowStockThresholdUseCase.Output =
            UpdateInventoryLowStockThresholdUseCase.Output.Success,
    ) : UpdateInventoryLowStockThresholdUseCase {
        var lastInput: UpdateInventoryLowStockThreshold? = null
        override suspend fun execute(
            input: UpdateInventoryLowStockThreshold,
        ): UpdateInventoryLowStockThresholdUseCase.Output {
            lastInput = input
            return output
        }
    }

    private fun viewModel(
        getUseCase: FakeGetInventoryItemUseCase,
        updateUseCase: FakeUpdateThresholdUseCase = FakeUpdateThresholdUseCase(),
    ) = PantryItemDetailViewModel(getUseCase, updateUseCase)

    private val sampleItem = InventoryItem(
        id = "i1",
        productId = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        imageUrl = "http://img/milk.png",
        quantity = 2,
        expiresInDays = null,
    )

    @Test
    fun `initial state is Loading before load is called`() {
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.NotFound)
        )

        assertTrue(viewModel.uiState.value is PantryItemDetailUiState.Loading)
    }

    @Test
    fun `load exposes Success with the item and forwards the id`() = runTest {
        val useCase = FakeGetInventoryItemUseCase(
            GetInventoryItemUseCase.Output.Success(sampleItem)
        )
        val viewModel = viewModel(useCase)

        viewModel.load("i1")

        val state = viewModel.uiState.value
        assertTrue(state is PantryItemDetailUiState.Success)
        assertEquals(sampleItem, (state as PantryItemDetailUiState.Success).item)
        assertEquals("i1", useCase.requestedId)
    }

    @Test
    fun `load exposes NotFound when the item is missing`() = runTest {
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.NotFound)
        )

        viewModel.load("missing")

        assertTrue(viewModel.uiState.value is PantryItemDetailUiState.NotFound)
    }

    @Test
    fun `load exposes Error when the lookup fails`() = runTest {
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(
                GetInventoryItemUseCase.Output.Failure(RuntimeException("boom"))
            )
        )

        viewModel.load("i1")

        assertTrue(viewModel.uiState.value is PantryItemDetailUiState.Error)
    }

    @Test
    fun `changing the threshold updates state and persists it`() = runTest {
        val update = FakeUpdateThresholdUseCase()
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.Success(sampleItem)),
            update,
        )
        viewModel.load("i1")

        viewModel.onLowStockThresholdChanged(3)

        val state = viewModel.uiState.value as PantryItemDetailUiState.Success
        assertEquals(3, state.item.lowStockThreshold)
        assertEquals(UpdateInventoryLowStockThreshold("p1", 3), update.lastInput)
    }

    @Test
    fun `clearing the threshold persists null`() = runTest {
        val update = FakeUpdateThresholdUseCase()
        val stocked = sampleItem.copy(lowStockThreshold = 3)
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.Success(stocked)),
            update,
        )
        viewModel.load("i1")

        viewModel.onLowStockThresholdChanged(null)

        val state = viewModel.uiState.value as PantryItemDetailUiState.Success
        assertNull(state.item.lowStockThreshold)
        assertEquals(UpdateInventoryLowStockThreshold("p1", null), update.lastInput)
    }

    @Test
    fun `an unchanged threshold is not persisted`() = runTest {
        val update = FakeUpdateThresholdUseCase()
        val stocked = sampleItem.copy(lowStockThreshold = 3)
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.Success(stocked)),
            update,
        )
        viewModel.load("i1")

        viewModel.onLowStockThresholdChanged(3)

        assertNull(update.lastInput)
    }

    @Test
    fun `a failed save reverts the threshold`() = runTest {
        val update = FakeUpdateThresholdUseCase(
            UpdateInventoryLowStockThresholdUseCase.Output.Failure(RuntimeException("boom")),
        )
        val viewModel = viewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.Success(sampleItem)),
            update,
        )
        viewModel.load("i1")

        viewModel.onLowStockThresholdChanged(5)

        val state = viewModel.uiState.value as PantryItemDetailUiState.Success
        assertNull(state.item.lowStockThreshold)
    }
}