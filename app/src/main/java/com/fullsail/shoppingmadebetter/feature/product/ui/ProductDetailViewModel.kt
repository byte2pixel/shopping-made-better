package com.fullsail.shoppingmadebetter.feature.product.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThreshold
import com.fullsail.shoppingmadebetter.feature.pantry.domain.UpdateInventoryLowStockThresholdUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.GetProductDetailUseCase
import com.fullsail.shoppingmadebetter.feature.product.domain.ProductDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Success(val product: ProductDetail) : ProductDetailUiState
    data object NotFound : ProductDetailUiState
    data object Error : ProductDetailUiState
}

/**
 * Backs the product detail screen, reached from a pantry lot row and from a History
 * line item alike.
 *
 * The low-stock threshold is written through the pantry feature's
 * [UpdateInventoryLowStockThresholdUseCase] on purpose: the setting is already keyed on
 * the product, not on an inventory row, so it is the same write either screen makes.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getProductDetailUseCase: GetProductDetailUseCase,
    private val updateInventoryLowStockThresholdUseCase: UpdateInventoryLowStockThresholdUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun load(productId: String) {
        _uiState.value = ProductDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val out = getProductDetailUseCase.execute(productId)) {
                is GetProductDetailUseCase.Output.Success -> ProductDetailUiState.Success(out.product)
                is GetProductDetailUseCase.Output.NotFound -> ProductDetailUiState.NotFound
                is GetProductDetailUseCase.Output.Failure -> ProductDetailUiState.Error
            }
        }
    }

    /**
     * Optimistically sets the shown product's low-stock [newThreshold] (`null` clears it),
     * persists it, and reverts if the save fails. No-op unless a product is loaded and the
     * value actually changes.
     */
    fun onLowStockThresholdChanged(newThreshold: Int?) {
        val current = _uiState.value as? ProductDetailUiState.Success ?: return
        val original = current.product
        if (newThreshold == original.lowStockThreshold) return

        _uiState.update { ProductDetailUiState.Success(original.copy(lowStockThreshold = newThreshold)) }
        viewModelScope.launch {
            val out = updateInventoryLowStockThresholdUseCase.execute(
                UpdateInventoryLowStockThreshold(original.id, newThreshold),
            )
            if (out is UpdateInventoryLowStockThresholdUseCase.Output.Failure) {
                // Revert only if the shown product is still the one we edited.
                _uiState.update { state ->
                    if (state is ProductDetailUiState.Success && state.product.id == original.id) {
                        ProductDetailUiState.Success(original)
                    } else {
                        state
                    }
                }
            }
        }
    }
}
