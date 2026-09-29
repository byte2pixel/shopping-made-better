package com.fullsail.shoppingmadebetter.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * A read-only icon-and-text value: no pill, no click, one colour. Use it where a
 * chip would promise a tap that does nothing; a [LabelChip] is for things that open.
 * A tap falls through to whatever the value sits in.
 *
 * @param contentDescription spoken instead of the visible [text]; null when the text
 *   already says it all, which leaves the text itself in the semantics tree.
 * @param color tints both the icon and the text; a Material role such as
 *   `onSurfaceVariant` or an accent that reads as text in both themes.
 * @param style the text style; `labelLarge` for a card header, `labelMedium` where
 *   the value replaces a chip.
 */
@Composable
fun InlineValue(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    @DrawableRes iconRes: Int? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val semantics = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Row(
        modifier = modifier.then(semantics),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(text = text, style = style, color = color)
    }
}
