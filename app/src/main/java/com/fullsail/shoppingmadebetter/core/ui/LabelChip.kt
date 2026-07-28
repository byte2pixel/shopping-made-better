package com.fullsail.shoppingmadebetter.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Opacity of a [LabelChip]'s fill wash relative to its accent color. */
private const val LabelChipFillAlpha = 0.15f

/** Opacity of a [LabelChip]'s border relative to its accent color. */
private const val LabelChipBorderAlpha = 0.5f

/**
 * A small pill "label" chip styled after GitHub's issue labels
 * a single [accentColor] drives the whole chip.
 * Feature-agnostic and stateless callers own the accent color and any action.
 * @param accentColor the base color. Pick one that reads as text over the
 *   surface behind the chip.
 *   Dark in a light theme, light in a dark theme.
 *
 *   Material roles such as `colorScheme.error` and `colorScheme.onSurfaceVariant`
 *   already satisfy this in both themes.
 * @param iconRes optional leading icon; tinted with [accentColor].
 * @param contentDescription spoken label replaces the visible [label] for screen readers
 * @param onClick when non-null, makes the chip a tappable button.
 */
@Composable
fun LabelChip(
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val interaction = when {
        onClick != null && contentDescription != null -> Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }

        onClick != null -> Modifier.clickable(role = Role.Button, onClick = onClick)

        contentDescription != null -> Modifier.clearAndSetSemantics {
            this.contentDescription = contentDescription
        }

        else -> Modifier
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = accentColor.copy(alpha = LabelChipFillAlpha),
        contentColor = accentColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = LabelChipBorderAlpha)),
        modifier = modifier.then(interaction),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
