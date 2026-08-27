package com.fullsail.shoppingmadebetter.feature.product.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.feature.pantry.ui.LowStockThresholdStepper
import com.fullsail.shoppingmadebetter.feature.product.domain.ProductDetail
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * One product's full record: what it is, how much of it the user has on hand, and the
 * settings that follow the product rather than any one pantry lot.
 * @param productId the `products.id` to show.
 * @param onTitleChange supplies the top-bar title once the product is known.
 */
@Composable
fun ProductDetailScreen(
    productId: String,
    modifier: Modifier = Modifier,
    onTitleChange: (String) -> Unit = {},
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(productId) { viewModel.load(productId) }

    val state = uiState
    if (state is ProductDetailUiState.Success) {
        LaunchedEffect(state.product.name) { onTitleChange(state.product.name) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            ProductDetailUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(
                    Alignment.Center
                )
            )

            ProductDetailUiState.Error -> CenteredMessage(
                message = stringResource(R.string.pantry_error),
                actionLabel = stringResource(R.string.pantry_retry),
                onAction = { viewModel.load(productId) },
            )

            ProductDetailUiState.NotFound -> CenteredMessage(message = stringResource(R.string.pantry_detail_not_found))
            is ProductDetailUiState.Success -> ProductDetailContent(
                product = state.product,
                onLowStockThresholdChange = viewModel::onLowStockThresholdChanged,
            )
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDetail,
    onLowStockThresholdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DetailField(label = stringResource(R.string.pantry_detail_brand), value = product.brand)
        DetailField(label = stringResource(R.string.pantry_detail_size), value = product.size)
        // Stock is how many of this product are in the pantry, across every lot of it.
        DetailField(
            label = stringResource(R.string.pantry_detail_stock),
            value = product.quantityOnHand.toString(),
        )
        if (product.description.isNotBlank()) {
            DetailField(
                label = stringResource(R.string.pantry_detail_description),
                value = product.description,
            )
        }

        HorizontalDivider()

        LowStockRow(
            threshold = product.lowStockThreshold,
            onThresholdChange = onLowStockThresholdChange,
        )
        DetailStubRow(label = stringResource(R.string.pantry_detail_stores))
        ExpirationRow(expiresInDays = product.expiresInDays)
    }
}

@Composable
private fun LowStockRow(
    threshold: Int?,
    onThresholdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.pantry_low_stock_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = threshold?.let { stringResource(R.string.pantry_low_stock_value, it) }
                    ?: stringResource(R.string.pantry_low_stock_off_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LowStockThresholdStepper(threshold = threshold, onThresholdChange = onThresholdChange)
    }
}

/** The soonest expiry across the product's lots, or that there is none to show. */
@Composable
private fun ExpirationRow(expiresInDays: Int?, modifier: Modifier = Modifier) {
    // Negative = overdue, 0 = due today, positive = days remaining, null = no known date.
    val isExpired = expiresInDays != null && expiresInDays < 0
    val value = when {
        expiresInDays == null -> stringResource(R.string.pantry_detail_expires_unknown)
        expiresInDays < 0 -> stringResource(R.string.pantry_detail_expired)
        expiresInDays == 0 -> stringResource(R.string.pantry_detail_expires_today)
        else -> pluralStringResource(
            R.plurals.pantry_detail_expires_in_days,
            expiresInDays,
            expiresInDays,
        )
    }
    val valueColor = when {
        isExpired -> MaterialTheme.colorScheme.error
        expiresInDays == null -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.pantry_detail_expiration),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun DetailField(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DetailStubRow(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.pantry_detail_coming_soon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(24.dp),
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onAction) { Text(text = actionLabel) }
            }
        }
    }
}

private fun previewProduct(
    expiresInDays: Int?,
    lowStockThreshold: Int? = 3,
    quantityOnHand: Int = 2,
) = ProductDetail(
    id = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    quantityOnHand = quantityOnHand,
    expiresInDays = expiresInDays,
    lowStockThreshold = lowStockThreshold,
)

@Preview(showBackground = true, name = "5 days left")
@Composable
private fun ProductDetailPreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(expiresInDays = 5),
            onLowStockThresholdChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Expired")
@Composable
private fun ProductDetailExpiredPreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(expiresInDays = -3, lowStockThreshold = null),
            onLowStockThresholdChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Bought before, no longer in the pantry")
@Composable
private fun ProductDetailNotHeldPreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(
                expiresInDays = null,
                lowStockThreshold = null,
                quantityOnHand = 0,
            ),
            onLowStockThresholdChange = {},
        )
    }
}
