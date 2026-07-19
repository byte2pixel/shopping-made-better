package com.fullsail.shoppingmadebetter.feature.pantry.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.GetInventoryItemUseCase
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private val sampleItem = InventoryItem(
        id = "i1",
        productId = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        imageUrl = "http://img/milk.png",
        quantity = 2,
    )

    @Test
    fun `initial state is Loading before load is called`() {
        val viewModel = PantryItemDetailViewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.NotFound)
        )

        assertTrue(viewModel.uiState.value is PantryItemDetailUiState.Loading)
    }

    @Test
    fun `load exposes Success with the item and forwards the id`() = runTest {
        val useCase = FakeGetInventoryItemUseCase(
            GetInventoryItemUseCase.Output.Success(sampleItem)
        )
        val viewModel = PantryItemDetailViewModel(useCase)

        viewModel.load("i1")

        val state = viewModel.uiState.value
        assertTrue(state is PantryItemDetailUiState.Success)
        assertEquals(sampleItem, (state as PantryItemDetailUiState.Success).item)
        assertEquals("i1", useCase.requestedId)
    }

    @Test
    fun `load exposes NotFound when the item is missing`() = runTest {
        val viewModel = PantryItemDetailViewModel(
            FakeGetInventoryItemUseCase(GetInventoryItemUseCase.Output.NotFound)
        )

        viewModel.load("missing")

        assertTrue(viewModel.uiState.value is PantryItemDetailUiState.NotFound)
    }

    @Test
    fun `load exposes Error when the lookup fails`() = runTest {
        val viewModel = PantryItemDetailViewModel(
            FakeGetInventoryItemUseCase(
                GetInventoryItemUseCase.Output.Failure(RuntimeException("boom"))
            )
        )

        viewModel.load("i1")

        assertTrue(viewModel.uiState.value is PantryItemDetailUiState.Error)
    }
}