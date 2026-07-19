package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ChangePasswordScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val password by viewModel.passwordInput.collectAsState()
    val confirmPassword by viewModel.confirmPasswordInput.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Change Password",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.onPasswordChanged(it) },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { viewModel.onConfirmPasswordChanged(it) },
            label = { Text("Confirm New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState) {
            is PasswordUiState.Loading -> CircularProgressIndicator()
            is PasswordUiState.Success -> {
                Text("Password changed successfully!", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }
            is PasswordUiState.Error -> {
                Text(
                    text = (uiState as PasswordUiState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            else -> {}
        }

        Button(
            onClick = { viewModel.executePasswordChange() },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState !is PasswordUiState.Loading
        ) {
            Text("Update Password")
        }

        TextButton(onClick = onNavigateBack) {
            Text("Cancel")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChangePasswordScreenPreview() {
    // We pass dummy lambda handlers just to see the layout visual
    Column {
        Text("Change Password Preview (Mock View)")
        // Renders the visual form layout shells
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("New Password") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}