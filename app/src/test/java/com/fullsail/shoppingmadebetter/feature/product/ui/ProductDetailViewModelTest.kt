package com.fullsail.shoppingmadebetter.feature.product.ui

import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.GetProductDetailUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.ProductDetail
import com.fullsail.shoppingmadebetter.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ProductDetailViewModel]. A hand-written fake
 * [GetProductDetailUseCase] drives each arm of the sealed output; the
 * [MainDispatcherRule] lets `viewModelScope` work run on the JVM.
 */
class ProductDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Fake use case: returns [output] and records the id it was asked for. */
    private class FakeGetProductDetailUseCase(
        var output: GetProductDetailUseCase.Output,
    ) : GetProductDetailUseCase {
        var requestedId: String? = null
        override suspend fun execute(input: String): GetProductDetailUseCase.Output {
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
        getUseCase: FakeGetProductDetailUseCase,
        updateUseCase: FakeUpdateThresholdUseCase = FakeUpdateThresholdUseCase(),
    ) = ProductDetailViewModel(getUseCase, updateUseCase)

    private val sampleProduct = ProductDetail(
        id = "p1",
        name = "Milk",
        brand = "Dairy Co",
        description = "2% milk",
        size = "1 gal",
        imageUrl = "http://img/milk.png",
        quantityOnHand = 2,
        expiresInDays = null,
    )

    @Test
    fun `initial state is Loading before load is called`() {
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.NotFound)
        )

        assertTrue(viewModel.uiState.value is ProductDetailUiState.Loading)
    }

    @Test
    fun `load exposes Success with the product and forwards the id`() = runTest {
        val useCase = FakeGetProductDetailUseCase(
            GetProductDetailUseCase.Output.Success(sampleProduct)
        )
        val viewModel = viewModel(useCase)

        viewModel.load("p1")

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Success)
        assertEquals(sampleProduct, (state as ProductDetailUiState.Success).product)
        assertEquals("p1", useCase.requestedId)
    }

    @Test
    fun `a product no longer in the pantry still loads`() = runTest {
        val notHeld = sampleProduct.copy(quantityOnHand = 0, expiresInDays = null)
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(notHeld))
        )

        viewModel.load("p1")

        val state = viewModel.uiState.value as ProductDetailUiState.Success
        assertEquals(0, state.product.quantityOnHand)
        assertNull(state.product.expiresInDays)
    }

    @Test
    fun `load exposes NotFound when the product is missing`() = runTest {
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.NotFound)
        )

        viewModel.load("missing")

        assertTrue(viewModel.uiState.value is ProductDetailUiState.NotFound)
    }

    @Test
    fun `load exposes Error when the lookup fails`() = runTest {
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(
                GetProductDetailUseCase.Output.Failure(RuntimeException("boom"))
            )
        )

        viewModel.load("p1")

        assertTrue(viewModel.uiState.value is ProductDetailUiState.Error)
    }

    @Test
    fun `changing the threshold updates state and persists it against the product`() = runTest {
        val update = FakeUpdateThresholdUseCase()
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(sampleProduct)),
            update,
        )
        viewModel.load("p1")

        viewModel.onLowStockThresholdChanged(3)

        val state = viewModel.uiState.value as ProductDetailUiState.Success
        assertEquals(3, state.product.lowStockThreshold)
        assertEquals(UpdateInventoryLowStockThreshold("p1", 3), update.lastInput)
    }

    @Test
    fun `clearing the threshold persists null`() = runTest {
        val update = FakeUpdateThresholdUseCase()
        val stocked = sampleProduct.copy(lowStockThreshold = 3)
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(stocked)),
            update,
        )
        viewModel.load("p1")

        viewModel.onLowStockThresholdChanged(null)

        val state = viewModel.uiState.value as ProductDetailUiState.Success
        assertNull(state.product.lowStockThreshold)
        assertEquals(UpdateInventoryLowStockThreshold("p1", null), update.lastInput)
    }

    @Test
    fun `an unchanged threshold is not persisted`() = runTest {
        val update = FakeUpdateThresholdUseCase()
        val stocked = sampleProduct.copy(lowStockThreshold = 3)
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(stocked)),
            update,
        )
        viewModel.load("p1")

        viewModel.onLowStockThresholdChanged(3)

        assertNull(update.lastInput)
    }

    @Test
    fun `a failed save reverts the threshold`() = runTest {
        val update = FakeUpdateThresholdUseCase(
            UpdateInventoryLowStockThresholdUseCase.Output.Failure(RuntimeException("boom")),
        )
        val viewModel = viewModel(
            FakeGetProductDetailUseCase(GetProductDetailUseCase.Output.Success(sampleProduct)),
            update,
        )
        viewModel.load("p1")

        viewModel.onLowStockThresholdChanged(5)

        val state = viewModel.uiState.value as ProductDetailUiState.Success
        assertNull(state.product.lowStockThreshold)
    }
}
