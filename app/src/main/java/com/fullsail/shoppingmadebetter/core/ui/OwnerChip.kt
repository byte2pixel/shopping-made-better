package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R

/**
 * Icon-only person chip for something a housemate owns. Rendered only when the thing
 * isn't the viewer's, so the icon alone says "someone else's" and keeps the row short;
 * tapping it opens an anchored popup reading [text], which is also the content
 * description. Callers supply the whole sentence ("Added by Sam", "Created by Sam") so
 * each feature keeps its own wording.
 */
@Composable
fun OwnerChip(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        LabelChip(
            label = null,
            accentColor = MaterialTheme.colorScheme.secondary,
            iconRes = R.drawable.ic_account_box,
            contentDescription = text,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}
