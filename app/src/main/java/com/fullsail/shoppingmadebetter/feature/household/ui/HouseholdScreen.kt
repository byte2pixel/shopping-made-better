package com.fullsail.shoppingmadebetter.feature.household.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.core.ui.LabelChip
import com.fullsail.shoppingmadebetter.feature.household.domain.Household
import com.fullsail.shoppingmadebetter.feature.household.domain.HouseholdMember
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme
import kotlinx.coroutines.launch

/** Create or join a household, see the current one and leave it, or manage it as its head. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HouseholdViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                HouseholdEvent.CreateFailed -> R.string.household_error_create
                HouseholdEvent.JoinInvalidCode -> R.string.household_error_invalid_code
                HouseholdEvent.AlreadyInHousehold -> R.string.household_error_already_member
                HouseholdEvent.JoinFailed -> R.string.household_error_join
                HouseholdEvent.LeaveRefusedHead -> R.string.household_error_leave_head
                HouseholdEvent.LeaveFailed -> R.string.household_error_leave
                HouseholdEvent.RenameFailed -> R.string.household_error_rename
                HouseholdEvent.TransferFailed -> R.string.household_error_transfer
                HouseholdEvent.RemoveFailed -> R.string.household_error_remove
                HouseholdEvent.RegenerateFailed -> R.string.household_error_regenerate
                HouseholdEvent.NotHead -> R.string.household_error_not_head
            }
            snackbarHostState.showSnackbar(resources.getString(message))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.household_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.household_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                HouseholdUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                HouseholdUiState.Error -> CenteredMessage(
                    message = stringResource(R.string.household_load_error),
                    actionLabel = stringResource(R.string.pantry_retry),
                    onAction = viewModel::load,
                )

                HouseholdUiState.None -> NoHouseholdContent(
                    busy = busy,
                    onCreate = viewModel::onCreate,
                    onJoin = viewModel::onJoin,
                )

                is HouseholdUiState.Member -> MemberContent(
                    household = state.household,
                    members = state.members,
                    busy = busy,
                    onCopyCode = { code ->
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(code, code)))
                            // Android 13+ shows its own "copied" confirmation.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                snackbarHostState.showSnackbar(resources.getString(R.string.household_code_copied))
                            }
                        }
                    },
                    onShareCode = { code ->
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.household_share_text, code))
                        }
                        context.startActivity(Intent.createChooser(send, resources.getString(R.string.household_share)))
                    },
                    onLeave = viewModel::onLeave,
                    onRename = viewModel::onRename,
                    onTransferHead = viewModel::onTransferHead,
                    onRemoveMember = viewModel::onRemoveMember,
                    onRegenerateCode = viewModel::onRegenerateCode,
                )
            }
        }
    }
}

@Composable
private fun NoHouseholdContent(
    busy: Boolean,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.household_create_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.household_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onCreate(name) },
                    enabled = name.isNotBlank() && !busy,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.household_create))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.household_join_title), style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.household_code_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onJoin(code) },
                    enabled = code.isNotBlank() && !busy,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.household_join))
                }
            }
        }
    }
}

/** The member view. Management controls show only when the caller is the head; the RPCs re-check. */
@Composable
private fun MemberContent(
    household: Household,
    members: List<HouseholdMember>,
    busy: Boolean,
    onCopyCode: (String) -> Unit,
    onShareCode: (String) -> Unit,
    onLeave: () -> Unit,
    onRename: (String) -> Unit,
    onTransferHead: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onRegenerateCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isHead = members.any { it.isSelf && it.isHead }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var confirmNewCode by rememberSaveable { mutableStateOf(false) }
    var removeTargetId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = household.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isHead) {
                IconButton(onClick = { showRename = true }, enabled = !busy) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.household_rename),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.household_invite_code),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = household.inviteCode,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onCopyCode(household.inviteCode) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_clone),
                    contentDescription = stringResource(R.string.household_copy),
                )
            }
            IconButton(onClick = { onShareCode(household.inviteCode) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.household_share),
                )
            }
            if (isHead) {
                IconButton(onClick = { confirmNewCode = true }, enabled = !busy) {
                    Icon(
                        painter = painterResource(R.drawable.ic_update),
                        contentDescription = stringResource(R.string.household_new_code),
                    )
                }
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.household_members),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        members.forEach { member ->
            MemberRow(
                member = member,
                canManage = isHead && !member.isSelf,
                enabled = !busy,
                onTransfer = { onTransferHead(member.id) },
                onRemove = { removeTargetId = member.id },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { confirmLeave = true },
            enabled = !busy,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(stringResource(R.string.household_leave), color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.household_leave_confirm_title)) },
            text = { Text(stringResource(R.string.household_leave_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeave = false
                        onLeave()
                    },
                ) {
                    Text(stringResource(R.string.household_leave), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) {
                    Text(stringResource(R.string.household_cancel))
                }
            },
        )
    }

    if (showRename) {
        RenameDialog(
            currentName = household.name,
            onDismiss = { showRename = false },
            onSave = { name ->
                showRename = false
                onRename(name)
            },
        )
    }

    if (confirmNewCode) {
        AlertDialog(
            onDismissRequest = { confirmNewCode = false },
            title = { Text(stringResource(R.string.household_new_code_confirm_title)) },
            text = { Text(stringResource(R.string.household_new_code_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmNewCode = false
                        onRegenerateCode()
                    },
                ) {
                    Text(stringResource(R.string.household_new_code))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewCode = false }) {
                    Text(stringResource(R.string.household_cancel))
                }
            },
        )
    }

    // A reload can drop the row while the dialog is up; then there is nothing to confirm.
    val removeTarget = removeTargetId?.let { id -> members.firstOrNull { it.id == id } }
    if (removeTarget != null) {
        AlertDialog(
            onDismissRequest = { removeTargetId = null },
            title = { Text(stringResource(R.string.household_remove_confirm_title, removeTarget.displayName)) },
            text = { Text(stringResource(R.string.household_remove_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeTargetId = null
                        onRemoveMember(removeTarget.id)
                    },
                ) {
                    Text(stringResource(R.string.household_remove_member), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { removeTargetId = null }) {
                    Text(stringResource(R.string.household_cancel))
                }
            },
        )
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }
    val trimmed = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.household_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.household_rename_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmed) },
                enabled = trimmed.isNotEmpty() && trimmed != currentName,
            ) {
                Text(stringResource(R.string.household_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.household_cancel))
            }
        },
    )
}

@Composable
private fun MemberRow(
    member: HouseholdMember,
    canManage: Boolean,
    enabled: Boolean,
    onTransfer: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = member.displayName,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (member.isSelf) {
            LabelChip(
                label = stringResource(R.string.household_you),
                accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (member.isHead) {
            LabelChip(
                label = stringResource(R.string.household_head),
                accentColor = MaterialTheme.colorScheme.primary,
            )
        }
        if (canManage) {
            Box {
                IconButton(onClick = { menuOpen = true }, enabled = enabled) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.household_member_menu),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.household_make_head)) },
                        onClick = {
                            menuOpen = false
                            onTransfer()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.household_remove_member)) },
                        onClick = {
                            menuOpen = false
                            onRemove()
                        },
                    )
                }
            }
        }
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

@Preview(showBackground = true)
@Composable
private fun NoHouseholdPreview() {
    ShoppingMadeBetterTheme {
        NoHouseholdContent(busy = false, onCreate = {}, onJoin = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun MemberPreview() {
    ShoppingMadeBetterTheme {
        MemberContent(
            household = Household(id = "h1", name = "Demo Household", inviteCode = "DEMO2026"),
            members = listOf(
                HouseholdMember(id = "u1", displayName = "Demo Shopper", isHead = true, isSelf = false),
                HouseholdMember(id = "u2", displayName = "Demo Roommate", isHead = false, isSelf = true),
            ),
            busy = false,
            onCopyCode = {},
            onShareCode = {},
            onLeave = {},
            onRename = {},
            onTransferHead = {},
            onRemoveMember = {},
            onRegenerateCode = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HeadPreview() {
    ShoppingMadeBetterTheme {
        MemberContent(
            household = Household(id = "h1", name = "Demo Household", inviteCode = "DEMO2026"),
            members = listOf(
                HouseholdMember(id = "u1", displayName = "Demo Shopper", isHead = true, isSelf = true),
                HouseholdMember(id = "u2", displayName = "Demo Roommate", isHead = false, isSelf = false),
            ),
            busy = false,
            onCopyCode = {},
            onShareCode = {},
            onLeave = {},
            onRename = {},
            onTransferHead = {},
            onRemoveMember = {},
            onRegenerateCode = {},
        )
    }
}
