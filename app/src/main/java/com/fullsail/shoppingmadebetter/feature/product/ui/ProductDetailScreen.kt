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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.ui.LowStockThresholdStepper
import com.fullsail.shoppingmadebetter.feature.pantry.ui.expiryStatLabel
import com.fullsail.shoppingmadebetter.feature.pantry.ui.iconRes
import com.fullsail.shoppingmadebetter.feature.pantry.ui.labelRes
import com.fullsail.shoppingmadebetter.feature.product.domain.ProductDetail
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/** Joins the brand with the size, and a lot's quantity with its location. */
private const val SEPARATOR = " · "

/**
 * One product's full record: what it is, the settings that follow the product rather
 * than any one pantry lot, and each lot of it the household holds.
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
        ProductHeader(product = product)
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

        HorizontalDivider()

        LotsSection(lots = product.lots)
    }
}

/** The product image beside one "brand · size" line; a blank half is left out. */
@Composable
private fun ProductHeader(product: ProductDetail, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductImage(imageUrl = product.imageUrl, contentDescription = null, size = 120.dp)
        val subtitle = listOf(product.brand, product.size)
            .filter { it.isNotBlank() }
            .joinToString(SEPARATOR)
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
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

/** Every lot of the product the household holds, or one line saying there is none. */
@Composable
private fun LotsSection(lots: List<InventoryItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.pantry_detail_lots, lots.size),
            style = MaterialTheme.typography.titleMedium,
        )
        if (lots.isEmpty()) {
            Text(
                text = stringResource(R.string.pantry_detail_no_lots),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            lots.forEach { lot -> LotDetailRow(lot = lot) }
        }
    }
}

/**
 * One lot as plain text: location icon, "Qty n · Where", the expiry at the end, and
 * who added it when it is a housemate's. Nothing here is tappable; edits stay on the
 * pantry card.
 */
@Composable
private fun LotDetailRow(lot: InventoryItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(lot.location.iconRes()),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.pantry_card_quantity, lot.quantity) +
                    SEPARATOR + stringResource(lot.location.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!lot.isOwn) {
                val name = lot.addedBy ?: stringResource(R.string.owner_chip_household)
                Text(
                    text = stringResource(R.string.pantry_lot_added_by, name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LotExpiry(expiresInDays = lot.expiresInDays)
    }
}

/**
 * A lot's expiry in the pantry card's words ("Expired", "Today", "7 days", "No date");
 * overdue in the error colour, undated muted.
 */
@Composable
private fun LotExpiry(expiresInDays: Int?, modifier: Modifier = Modifier) {
    val text = if (expiresInDays == null) {
        stringResource(R.string.pantry_card_expiry_none)
    } else {
        expiryStatLabel(expiresInDays)
    }
    val color = when {
        expiresInDays == null -> MaterialTheme.colorScheme.onSurfaceVariant
        expiresInDays < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier,
    )
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

private fun previewLot(
    id: String,
    expiresInDays: Int?,
    location: PantryLocation = PantryLocation.Fridge,
    addedBy: String? = null,
) = InventoryItem(
    id = id,
    productId = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    quantity = 1,
    expiresInDays = expiresInDays,
    location = location,
    addedBy = addedBy,
    isOwn = addedBy == null,
)

private fun previewProduct(
    lots: List<InventoryItem>,
    lowStockThreshold: Int? = 3,
) = ProductDetail(
    id = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    lots = lots,
    lowStockThreshold = lowStockThreshold,
)

@Preview(showBackground = true, name = "Two lots, 5 days left")
@Composable
private fun ProductDetailPreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(
                lots = listOf(previewLot("l1", 5), previewLot("l2", 12, PantryLocation.Freezer)),
            ),
            onLowStockThresholdChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Expired")
@Composable
private fun ProductDetailExpiredPreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(lots = listOf(previewLot("l1", -3)), lowStockThreshold = null),
            onLowStockThresholdChange = {},
        )
    }
}

@Preview(showBackground = true, name = "A housemate's lot")
@Composable
private fun ProductDetailHousematePreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(
                lots = listOf(previewLot("l1", -1), previewLot("l2", 8, addedBy = "Demo Roommate")),
            ),
            onLowStockThresholdChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Bought before, no longer in the pantry")
@Composable
private fun ProductDetailNotHeldPreview() {
    ShoppingMadeBetterTheme {
        ProductDetailContent(
            product = previewProduct(lots = emptyList(), lowStockThreshold = null),
            onLowStockThresholdChange = {},
        )
    }
}
