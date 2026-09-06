package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fullsail.shoppingmadebetter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToChangePassword: () -> Unit,
    onNavigateBack: () -> Unit,
    onEditPreferences: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val autoAdjustEnabled by viewModel.autoAdjustEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val autoAdjustFailedMessage = stringResource(R.string.profile_auto_adjust_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileSettingsEvent.AutoAdjustUpdateFailed ->
                    snackbarHostState.showSnackbar(autoAdjustFailedMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedButton(
                onClick = onEditPreferences,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Dietary Preferences & Goals")
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back), // Or appropriate forward arrow icon
                        contentDescription = "Edit Preferences"
                    )
                }
            }

            Divider()

            Text(
                text = stringResource(R.string.profile_pantry_header),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            AutoAdjustRow(
                enabled = autoAdjustEnabled,
                onToggled = viewModel::onAutoAdjustToggled,
            )

            Divider()

            Text(
                text = "Account Security",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = onNavigateToChangePassword,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = { /* TODO: Ticketed for future sprint - Handle logout */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Sign Out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** The auto-adjust switch as one toggleable row; disabled until [enabled] is known. */
@Composable
private fun AutoAdjustRow(enabled: Boolean?, onToggled: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = enabled == true,
                enabled = enabled != null,
                role = Role.Switch,
                onValueChange = onToggled,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_auto_adjust_title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.profile_auto_adjust_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled == true,
            onCheckedChange = null,
            enabled = enabled != null,
        )
    }
}
