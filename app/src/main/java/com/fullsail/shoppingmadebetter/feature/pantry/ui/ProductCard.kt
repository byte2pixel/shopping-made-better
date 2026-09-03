package com.fullsail.shoppingmadebetter.feature.pantry.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.core.ui.ProductImage
import com.fullsail.shoppingmadebetter.core.ui.Stepper
import com.fullsail.shoppingmadebetter.feature.pantry.domain.EstimateSource
import com.fullsail.shoppingmadebetter.feature.pantry.domain.InventoryItem
import com.fullsail.shoppingmadebetter.feature.pantry.domain.PantryLocation
import com.fullsail.shoppingmadebetter.feature.pantry.domain.ProductGroup
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import com.fullsail.shoppingmadebetter.ui.theme.expirySoonAccentDark
import com.fullsail.shoppingmadebetter.ui.theme.expirySoonAccentLight
import com.fullsail.shoppingmadebetter.ui.theme.warningAccentDark
import com.fullsail.shoppingmadebetter.ui.theme.warningAccentLight

/**
 * Severity of an item's remaining shelf life, used to color the expiry chip.
 * See [expiryBucket] for the day thresholds each bucket covers.
 */
enum class ExpiryBucket { Expired, Urgent, Soon, Later }

/**
 * Maps days-until-expiry to a chip severity. Mirrors [EXPIRING_SOON_DAYS]
 *
 * - `<= 0` -> [ExpiryBucket.Expired] (red)  — already past its date or due today
 * - `1..2` -> [ExpiryBucket.Urgent] (orange)
 * - `3..5` -> [ExpiryBucket.Soon] (yellow)
 * - `6+`   -> [ExpiryBucket.Later] (grey) — plenty of time, but still adjustable
 */
internal fun expiryBucket(expiresInDays: Int): ExpiryBucket = when {
    expiresInDays <= 0 -> ExpiryBucket.Expired
    expiresInDays <= 2 -> ExpiryBucket.Urgent
    expiresInDays <= EXPIRING_SOON_DAYS -> ExpiryBucket.Soon
    else -> ExpiryBucket.Later
}

/**
 * On-hand severity of an item, used to color the quantity chip and drive the
 * dashboard's "Out"/"Running Low" filters. See [stockLevel] for the thresholds.
 */
enum class StockLevel { Out, Low, Ok }

/**
 * Maps an item's on-hand [quantity] and its per-item [lowStockThreshold] to a severity.
 *
 * - `<= 0`                    -> [StockLevel.Out] (red) — none on hand
 * - `1..lowStockThreshold`    -> [StockLevel.Low] (orange) — running low
 * - otherwise / no threshold  -> [StockLevel.Ok] (grey) — plenty on hand
 *
 * Items without a [lowStockThreshold] are never [StockLevel.Low].
 */
internal fun stockLevel(quantity: Int, lowStockThreshold: Int?): StockLevel = when {
    quantity <= 0 -> StockLevel.Out
    lowStockThreshold != null && quantity <= lowStockThreshold -> StockLevel.Low
    else -> StockLevel.Ok
}

/** The chip accent color for a stock severity. */
@Composable
private fun stockAccent(level: StockLevel): Color {
    val dark = isSystemInDarkTheme()
    return when (level) {
        StockLevel.Out -> MaterialTheme.colorScheme.error
        StockLevel.Low -> if (dark) warningAccentDark else warningAccentLight
        StockLevel.Ok -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** The chip accent color for an expiry severity. */
@Composable
private fun expiryAccent(bucket: ExpiryBucket): Color {
    val dark = isSystemInDarkTheme()
    return when (bucket) {
        ExpiryBucket.Expired -> MaterialTheme.colorScheme.error
        ExpiryBucket.Urgent -> if (dark) warningAccentDark else warningAccentLight
        ExpiryBucket.Soon -> if (dark) expirySoonAccentDark else expirySoonAccentLight
        ExpiryBucket.Later -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** The compact expiry chip label: "Expired", "Today", or a short day count. */
@Composable
private fun expiryChipLabel(expiresInDays: Int): String = when {
    expiresInDays < 0 -> stringResource(R.string.pantry_expiry_expired)
    expiresInDays == 0 -> stringResource(R.string.pantry_expiry_today)
    else -> stringResource(R.string.pantry_expiry_in_days_short, expiresInDays)
}

/** The spoken expiry chip description, spelling out what the short label means. */
@Composable
private fun expiryChipDescription(expiresInDays: Int): String = when {
    expiresInDays < 0 -> stringResource(R.string.pantry_detail_expired)
    expiresInDays == 0 -> stringResource(R.string.pantry_detail_expires_today)
    else -> pluralStringResource(
        R.plurals.pantry_detail_expires_in_days,
        expiresInDays,
        expiresInDays,
    )
}

/** The spring driving the expander and chevron */
private fun <T> expandSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * One pantry product rendered as a card. The header aggregates every lot of the
 * product — total quantity, soonest expiry, shared (or "Mixed") location — and
 * tapping it expands the card to one row per lot for per-lot editing.
 *
 * Per-product actions (add to list, the low-stock threshold behind the total
 * quantity chip) live in the header; quantity/expiry/location edits and removal
 * act on a single lot from its row.
 *
 * @param isExpanded whether the lot rows are showing; hoisted so the caller owns it.
 * @param onExpandedChange requests the new expanded state after a header tap.
 * @param onLotClick opens the detail screen for the tapped lot's product.
 * @param onAddToList opens the "add to shopping list" flow for this product.
 * @param onRemoveLot requests removal of one lot from the pantry.
 * @param onQuantityChange requests persisting a new on-hand quantity for one lot.
 * @param onLocationChange requests persisting a new storage location for one lot.
 * @param onExpiryChange requests persisting a new shelf life (days from today) for one lot.
 * @param onLowStockThresholdChange requests persisting the product's low-stock threshold
 *   (`null` clears it).
 * @param onConfirmEstimate confirms one lot's auto-adjusted quantity as-is.
 * @param onCorrectEstimate replaces one lot's auto-adjusted quantity with the given count.
 */
@Composable
fun ProductCard(
    group: ProductGroup,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onLotClick: (InventoryItem) -> Unit,
    onAddToList: () -> Unit,
    onRemoveLot: (InventoryItem) -> Unit,
    onQuantityChange: (InventoryItem, Int) -> Unit,
    onLocationChange: (InventoryItem, PantryLocation) -> Unit,
    onExpiryChange: (InventoryItem, Int) -> Unit,
    onLowStockThresholdChange: (Int?) -> Unit,
    onConfirmEstimate: (InventoryItem) -> Unit,
    onCorrectEstimate: (InventoryItem, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            ProductCardHeader(
                group = group,
                isExpanded = isExpanded,
                onToggleExpanded = { onExpandedChange(!isExpanded) },
                onAddToList = onAddToList,
                onLowStockThresholdChange = onLowStockThresholdChange,
            )
            // Expand with a light spring settle; collapse critically damped — a height
            // bounce reads as broken while content is being clipped away.
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = expandSpring(),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 50)),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(durationMillis = 120)),
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    HorizontalDivider()
                    group.lots.forEach { lot ->
                        LotRow(
                            lot = lot,
                            onClick = { onLotClick(lot) },
                            onRemove = { onRemoveLot(lot) },
                            onQuantityChange = { newQuantity -> onQuantityChange(lot, newQuantity) },
                            onLocationChange = { newLocation -> onLocationChange(lot, newLocation) },
                            onExpiryChange = { newDays -> onExpiryChange(lot, newDays) },
                            onConfirmEstimate = { onConfirmEstimate(lot) },
                            onCorrectEstimate = { newQuantity -> onCorrectEstimate(lot, newQuantity) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The always-visible top of a [ProductCard]: product image and details, the
 * add-to-list action, a rotating chevron, and the aggregate indicator chips.
 * Tapping anywhere on it (outside the buttons and the quantity chip) toggles
 * the lot rows via [onToggleExpanded].
 */
@Composable
private fun ProductCardHeader(
    group: ProductGroup,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddToList: () -> Unit,
    onLowStockThresholdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggleLabel = stringResource(
        if (isExpanded) R.string.pantry_card_collapse_lots else R.string.pantry_card_expand_lots
    )
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = expandSpring(),
        label = "chevron-rotation",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = toggleLabel, onClick = onToggleExpanded)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductImage(imageUrl = group.imageUrl, contentDescription = group.name)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(text = group.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = group.brand,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = group.size,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onAddToList) {
                Icon(
                    painter = painterResource(R.drawable.ic_shopping_cart),
                    contentDescription = stringResource(R.string.pantry_add_to_list),
                )
            }
            // Announced through the header's toggle label, so decorative here.
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation),
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TotalQuantityChip(
                totalQuantity = group.totalQuantity,
                lowStockThreshold = group.lowStockThreshold,
                onLowStockThresholdChange = onLowStockThresholdChange,
            )
            HeaderLocationChip(group = group)
            group.earliestExpiresInDays?.let { days -> HeaderExpiryChip(expiresInDays = days) }
        }
    }
}

/**
 * One lot of the expanded card: the same quantity/location/expiry quick-action
 * chips as before, each committing against this lot, plus a per-lot remove.
 * Tapping the row itself (outside the chips) opens the product's detail screen.
 * An auto-adjusted lot also shows an "est." chip that toggles an inline
 * confirm-or-fix row underneath.
 */
@Composable
private fun LotRow(
    lot: InventoryItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onLocationChange: (PantryLocation) -> Unit,
    onExpiryChange: (Int) -> Unit,
    onConfirmEstimate: () -> Unit,
    onCorrectEstimate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEstimateConfirm by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClickLabel = stringResource(R.string.pantry_card_lot_details),
                    onClick = onClick,
                )
                .padding(start = 12.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LotQuantityChip(quantity = lot.quantity, onQuantityChange = onQuantityChange)
            if (lot.estimated) {
                EstimateChip(
                    estimateSource = lot.estimateSource,
                    onClick = { showEstimateConfirm = !showEstimateConfirm },
                )
            }
            LocationChip(location = lot.location, onLocationChange = onLocationChange)
            lot.expiresInDays?.let { days ->
                ExpiryChip(
                    bucket = expiryBucket(days),
                    expiresInDays = days,
                    onExpiryChange = onExpiryChange,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.pantry_card_remove_lot),
                )
            }
        }
        AnimatedVisibility(visible = lot.estimated && showEstimateConfirm) {
            EstimateConfirmRow(
                quantity = lot.quantity,
                onConfirm = {
                    showEstimateConfirm = false
                    onConfirmEstimate()
                },
                onCorrect = { newQuantity ->
                    showEstimateConfirm = false
                    onCorrectEstimate(newQuantity)
                },
            )
        }
    }
}

/**
 * The "est." marker on an auto-adjusted lot. The label stays terse; the spoken
 * description carries the estimate's basis. Tapping it toggles the lot's
 * confirm-or-fix row.
 */
@Composable
private fun EstimateChip(
    estimateSource: EstimateSource?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LabelChip(
        label = stringResource(R.string.pantry_estimate_chip),
        accentColor = MaterialTheme.colorScheme.tertiary,
        contentDescription = stringResource(
            when (estimateSource) {
                EstimateSource.History -> R.string.pantry_estimate_desc_history
                EstimateSource.ShelfLife -> R.string.pantry_estimate_desc_shelf_life
                else -> R.string.pantry_estimate_desc_generic
            },
        ),
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * The inline check under an estimated lot: "We estimated N left. Correct?".
 * Yes confirms the number as-is via [onConfirm]; Fix opens the quantity stepper
 * and commits the corrected count via [onCorrect] when the popup closes.
 * Dismissing the popup unchanged commits nothing — that's a cancel, not a confirm.
 */
@Composable
private fun EstimateConfirmRow(
    quantity: Int,
    onConfirm: () -> Unit,
    onCorrect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fixExpanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(quantity) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.pantry_estimate_confirm_question, quantity),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onConfirm) {
            Text(text = stringResource(R.string.pantry_estimate_confirm_yes))
        }
        Box {
            TextButton(
                onClick = {
                    draft = quantity
                    fixExpanded = true
                },
            ) {
                Text(text = stringResource(R.string.pantry_estimate_confirm_fix))
            }
            QuantityStepperPopup(
                expanded = fixExpanded,
                labelRes = R.string.pantry_quantity_edit_label,
                draft = draft,
                onDraftChange = { draft = it },
                onDismissRequest = {
                    fixExpanded = false
                    if (draft != quantity) onCorrect(draft)
                },
            )
        }
    }
}

/** The drawable icon representing where an item is stored. */
@DrawableRes
internal fun PantryLocation.iconRes(): Int = when (this) {
    PantryLocation.Freezer -> R.drawable.ic_freezer
    PantryLocation.Fridge -> R.drawable.ic_fridge
    PantryLocation.Pantry -> R.drawable.ic_pantry
}

/** The display name for a storage location, shared with the pantry dashboard. */
@StringRes
internal fun PantryLocation.labelRes(): Int = when (this) {
    PantryLocation.Freezer -> R.string.pantry_dashboard_freezer
    PantryLocation.Fridge -> R.string.pantry_dashboard_fridge
    PantryLocation.Pantry -> R.string.pantry_dashboard_pantry
}

/** The three storage locations, in the order they're offered in the location picker. */
private val locationChoices = listOf(
    PantryLocation.Pantry,
    PantryLocation.Fridge,
    PantryLocation.Freezer,
)

/**
 * The header's location aggregate.
 */
@Composable
private fun HeaderLocationChip(group: ProductGroup, modifier: Modifier = Modifier) {
    val single = group.singleLocation
    if (single != null) {
        val label = stringResource(single.labelRes())
        LabelChip(
            label = label,
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconRes = single.iconRes(),
            contentDescription = stringResource(R.string.pantry_card_location_desc, label),
            modifier = modifier,
        )
    } else {
        LabelChip(
            label = stringResource(R.string.pantry_location_mixed),
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = stringResource(R.string.pantry_card_location_mixed_desc),
            modifier = modifier,
        )
    }
}

/**
 * The header's expiry aggregate.
 */
@Composable
private fun HeaderExpiryChip(expiresInDays: Int, modifier: Modifier = Modifier) {
    LabelChip(
        label = expiryChipLabel(expiresInDays),
        accentColor = expiryAccent(expiryBucket(expiresInDays)),
        iconRes = R.drawable.ic_expiring,
        contentDescription = stringResource(
            R.string.pantry_card_expiry_soonest_desc,
            expiryChipDescription(expiresInDays),
        ),
        modifier = modifier,
    )
}

/**
 * The location chip. Tapping it opens an anchored popup listing the storage locations.
 * Picking one commits it via [onLocationChange] (nothing is sent when it's unchanged).
 */
@Composable
private fun LocationChip(
    location: PantryLocation,
    onLocationChange: (PantryLocation) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(location.labelRes())

    Box(modifier = modifier) {
        LabelChip(
            label = label,
            accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconRes = location.iconRes(),
            contentDescription = stringResource(R.string.pantry_card_location_desc, label),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            locationChoices.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(stringResource(choice.labelRes())) },
                    leadingIcon = {
                        Icon(painter = painterResource(choice.iconRes()), contentDescription = null)
                    },
                    trailingIcon = {
                        RadioButton(selected = choice == location, onClick = null)
                    },
                    onClick = {
                        expanded = false
                        onLocationChange(choice)
                    },
                )
            }
        }
    }
}

/**
 * The header's total-quantity chip: how many are on hand across every lot, colored
 * by the product's stock severity. Tapping it opens an anchored popup with the
 * product's low-stock threshold stepper — the one per-product setting — committed
 * via [onLowStockThresholdChange] when the popup closes (only when changed).
 */
@Composable
private fun TotalQuantityChip(
    totalQuantity: Int,
    lowStockThreshold: Int?,
    onLowStockThresholdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var thresholdDraft by remember { mutableStateOf(lowStockThreshold) }

    Box(modifier = modifier) {
        LabelChip(
            label = stringResource(R.string.pantry_card_total_quantity, totalQuantity),
            accentColor = stockAccent(stockLevel(totalQuantity, lowStockThreshold)),
            iconRes = R.drawable.ic_add,
            contentDescription = pluralStringResource(
                R.plurals.pantry_card_total_quantity_desc,
                totalQuantity,
                totalQuantity,
            ),
            onClick = {
                thresholdDraft = lowStockThreshold
                expanded = true
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                onLowStockThresholdChange(thresholdDraft)
            },
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.pantry_low_stock_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LowStockThresholdStepper(
                    threshold = thresholdDraft,
                    onThresholdChange = { thresholdDraft = it },
                )
            }
        }
    }
}

/**
 * A lot row's quantity chip. Tapping it opens an anchored popup with a quantity
 * [Stepper]; the new value is committed via [onQuantityChange] when the popup
 * closes, and only when actually changed. Quantity floors at 0 — that's "out of
 * stock", and an empty lot is the one severity this chip shows.
 * Running low is a property of the product, not of one lot — the threshold is
 * per-product and the quantity that answers it is the total across every lot — so it
 * is colored once, on the header's [TotalQuantityChip], and never here.
 */
@Composable
private fun LotQuantityChip(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(quantity) }

    Box(modifier = modifier) {
        LabelChip(
            label = stringResource(R.string.pantry_card_quantity, quantity),
            accentColor = stockAccent(stockLevel(quantity, lowStockThreshold = null)),
            iconRes = R.drawable.ic_add,
            contentDescription = pluralStringResource(
                R.plurals.pantry_card_quantity_desc,
                quantity,
                quantity,
            ),
            onClick = {
                draft = quantity
                expanded = true
            },
        )
        QuantityStepperPopup(
            expanded = expanded,
            labelRes = R.string.pantry_quantity_edit_label,
            draft = draft,
            onDraftChange = { draft = it },
            onDismissRequest = {
                expanded = false
                onQuantityChange(draft)
            },
        )
    }
}

/**
 * The anchored quantity-stepper popup shared by [LotQuantityChip] and
 * [EstimateConfirmRow]'s Fix action. The caller owns the draft and decides what
 * to commit on [onDismissRequest].
 */
@Composable
private fun QuantityStepperPopup(
    expanded: Boolean,
    @StringRes labelRes: Int,
    draft: Int,
    onDraftChange: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Stepper(
                valueLabel = draft.toString(),
                onDecrement = { if (draft > MIN_QUANTITY) onDraftChange(draft - 1) },
                onIncrement = { onDraftChange(draft + 1) },
                decrementContentDescription = stringResource(R.string.pantry_quantity_decrease),
                incrementContentDescription = stringResource(R.string.pantry_quantity_increase),
                decrementEnabled = draft > MIN_QUANTITY,
            )
        }
    }
}

/** Quantity floors here from the chip: 0 means out of stock (still wanted). */
private const val MIN_QUANTITY = 0

/** Days added by each expiry quick-add button, in the order shown. */
private val expiryQuickAddDays = listOf(1, 3, 7)

/**
 * The expiry chip. Tapping it opens an anchored popup with a day [Stepper] plus quick-add
 * buttons; the new shelf life (days from today) is committed via [onExpiryChange] when the
 * popup closes (nothing is sent when unchanged). Decrement floors at "today" (0 days).
 */
@Composable
private fun ExpiryChip(
    bucket: ExpiryBucket,
    expiresInDays: Int,
    onExpiryChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember { mutableIntStateOf(expiresInDays) }

    Box(modifier = modifier) {
        LabelChip(
            label = expiryChipLabel(expiresInDays),
            accentColor = expiryAccent(bucket),
            iconRes = R.drawable.ic_expiring,
            contentDescription = expiryChipDescription(expiresInDays),
            onClick = {
                draft = expiresInDays
                expanded = true
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                onExpiryChange(draft)
            },
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.pantry_expiry_edit_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    valueLabel = expiryDraftLabel(draft),
                    onDecrement = { if (draft > 0) draft-- },
                    onIncrement = { draft++ },
                    decrementContentDescription = stringResource(R.string.pantry_expiry_days_decrease),
                    incrementContentDescription = stringResource(R.string.pantry_expiry_days_increase),
                    decrementEnabled = draft > 0,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    expiryQuickAddDays.forEach { days ->
                        TextButton(onClick = { draft += days }) {
                            Text(text = stringResource(R.string.pantry_expiry_add_days, days))
                        }
                    }
                }
            }
        }
    }
}

/** The stepper's value label for [days] until expiry: "Today" at 0, otherwise a day count. */
@Composable
private fun expiryDraftLabel(days: Int): String = if (days == 0) {
    stringResource(R.string.pantry_expiry_today)
} else {
    pluralStringResource(R.plurals.pantry_expiry_days, days, days)
}

private fun previewLot(
    id: String,
    quantity: Int = 2,
    expiresInDays: Int? = 4,
    location: PantryLocation = PantryLocation.Pantry,
    lowStockThreshold: Int? = null,
    estimated: Boolean = false,
    estimateSource: EstimateSource? = null,
) = InventoryItem(
    id = id,
    productId = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    quantity = quantity,
    expiresInDays = expiresInDays,
    location = location,
    lowStockThreshold = lowStockThreshold,
    estimated = estimated,
    estimateSource = estimateSource,
)

private fun previewGroup(lots: List<InventoryItem>) = ProductGroup(
    productId = "p1",
    name = "2% Milk",
    brand = "Great Value",
    description = "Reduced-fat milk, one gallon.",
    size = "1 gal",
    imageUrl = "",
    lots = lots,
)

@Composable
private fun ProductCardPreviewScaffold(group: ProductGroup, isExpanded: Boolean) {
    ShoppingMadeBetterTheme {
        ProductCard(
            group = group,
            isExpanded = isExpanded,
            onExpandedChange = {},
            onLotClick = {},
            onAddToList = {},
            onRemoveLot = {},
            onQuantityChange = { _, _ -> },
            onLocationChange = { _, _ -> },
            onExpiryChange = { _, _ -> },
            onLowStockThresholdChange = {},
            onConfirmEstimate = {},
            onCorrectEstimate = { _, _ -> },
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Single lot, collapsed")
@Composable
private fun ProductCardCollapsedPreview() {
    ProductCardPreviewScaffold(
        group = previewGroup(listOf(previewLot(id = "1", location = PantryLocation.Fridge))),
        isExpanded = false,
    )
}

@Preview(showBackground = true, name = "Mixed locations, expanded")
@Composable
private fun ProductCardExpandedPreview() {
    ProductCardPreviewScaffold(
        group = previewGroup(
            listOf(
                previewLot(id = "1", quantity = 1, expiresInDays = 2, location = PantryLocation.Fridge),
                previewLot(id = "2", quantity = 3, expiresInDays = 9, location = PantryLocation.Freezer),
            ),
        ),
        isExpanded = true,
    )
}

@Preview(showBackground = true, name = "Expired soonest lot")
@Composable
private fun ProductCardExpiredPreview() {
    ProductCardPreviewScaffold(
        group = previewGroup(
            listOf(
                previewLot(id = "1", expiresInDays = -2, location = PantryLocation.Freezer),
                previewLot(id = "2", expiresInDays = 12, location = PantryLocation.Freezer),
            ),
        ),
        isExpanded = true,
    )
}

@Preview(showBackground = true, name = "Running low, expanded")
@Composable
private fun ProductCardLowStockPreview() {
    // Three on hand against a threshold of five: the header reads low, while the lot
    // chips stay neutral except the empty lot, which is out on its own.
    ProductCardPreviewScaffold(
        group = previewGroup(
            listOf(
                previewLot(id = "1", quantity = 1, expiresInDays = 2, lowStockThreshold = 5),
                previewLot(id = "2", quantity = 2, expiresInDays = 6, lowStockThreshold = 5),
                previewLot(id = "3", quantity = 0, expiresInDays = 14, lowStockThreshold = 5),
            ),
        ),
        isExpanded = true,
    )
}

@Preview(showBackground = true, name = "Estimated lot, expanded")
@Composable
private fun ProductCardEstimatedPreview() {
    ProductCardPreviewScaffold(
        group = previewGroup(
            listOf(
                previewLot(
                    id = "1",
                    quantity = 1,
                    estimated = true,
                    estimateSource = EstimateSource.History,
                ),
                previewLot(id = "2", quantity = 3, expiresInDays = 9),
            ),
        ),
        isExpanded = true,
    )
}

@Preview(showBackground = true, name = "No expiry date")
@Composable
private fun ProductCardNoExpiryPreview() {
    ProductCardPreviewScaffold(
        group = previewGroup(listOf(previewLot(id = "1", expiresInDays = null))),
        isExpanded = false,
    )
}
