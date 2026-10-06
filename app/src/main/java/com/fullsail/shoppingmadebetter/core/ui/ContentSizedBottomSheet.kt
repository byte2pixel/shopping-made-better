package com.fullsail.shoppingmadebetter.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch

/**
 * A [ModalBottomSheet] that opens at the height of its content and follows it when the
 * content grows, with no half-open stop.
 *
 * It re-expands on every height change because the stock sheet can miss one: content that
 * grows on the frame the open animation ends leaves the sheet at its old height, with the
 * new rows below the screen and dragging disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSizedBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    // Last measured height; 0 until the first measure, which the sheet opens to by itself.
    val lastHeight = remember { intArrayOf(0) }
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(
            modifier = Modifier.onSizeChanged { size ->
                val changed = lastHeight[0] != 0 && lastHeight[0] != size.height
                lastHeight[0] = size.height
                // A sheet on its way out stays on its way out.
                if (changed && sheetState.targetValue != SheetValue.Hidden) {
                    scope.launch { sheetState.expand() }
                }
            },
            content = content,
        )
    }
}
