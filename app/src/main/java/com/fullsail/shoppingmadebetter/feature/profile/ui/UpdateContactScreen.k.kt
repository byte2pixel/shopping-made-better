package com.fullsail.shoppingmadebetter.feature.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateContactScreen(
    viewModel: UpdateContactViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailInput by viewModel.emailInput.collectAsState()
    val phoneInput by viewModel.phoneInput.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Email / Phone") },
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
            if (uiState is ContactUiState.Error) {
                Text(
                    text = (uiState as ContactUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("New Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phoneInput,
                onValueChange = viewModel::onPhoneChanged,
                label = { Text("New Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = viewModel::executeContactUpdate,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is ContactUiState.Loading
            ) {
                if (uiState is ContactUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}