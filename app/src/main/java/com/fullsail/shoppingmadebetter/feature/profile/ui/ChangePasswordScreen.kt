package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@Composable
fun ChangePasswordScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val passwordInput by viewModel.passwordInput.collectAsState()
    val confirmPasswordInput by viewModel.confirmPasswordInput.collectAsState()

    ChangePasswordContent(
        uiState = uiState,
        passwordInput = passwordInput,
        confirmPasswordInput = confirmPasswordInput,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onSubmit = viewModel::executePasswordChange,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordContent(
    uiState: PasswordUiState,
    passwordInput: String,
    confirmPasswordInput: String,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is PasswordUiState.Error) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = passwordInput,
                onValueChange = onPasswordChanged,
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = onConfirmPasswordChanged,
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is PasswordUiState.Loading
            ) {
                if (uiState is PasswordUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Update Password")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    ShoppingMadeBetterTheme {
        ChangePasswordContent(
            uiState = PasswordUiState.Idle,
            passwordInput = "",
            confirmPasswordInput = "",
            onPasswordChanged = {},
            onConfirmPasswordChanged = {},
            onSubmit = {},
            onNavigateBack = {}
        )
    }
}