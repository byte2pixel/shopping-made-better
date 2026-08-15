package com.fullsail.shoppingmadebetter.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.fullsail.shoppingmadebetter.R
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

/**
 * Sign-in screen: collects email + password and authenticates through
 * [SignInViewModel] -> SignInUseCase -> Supabase Auth. [onSignedIn] fires once the
 * sign-in succeeds; [onNavigateToSignUp] routes to the registration screen.
 */
@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is SignInUiState.Success) onSignedIn()
    }

    LoginContent(
        uiState = uiState,
        onSignIn = viewModel::signIn,
        onNavigateToSignUp = onNavigateToSignUp,
        modifier = modifier,
    )
}

@Composable
private fun LoginContent(
    uiState: SignInUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Theme roles keep the green branding while staying legible in light and dark mode.
    val pageColor = MaterialTheme.colorScheme.primaryContainer
    val cardColor = MaterialTheme.colorScheme.surface
    val activePill = MaterialTheme.colorScheme.primary
    val onActivePill = MaterialTheme.colorScheme.onPrimary
    val inactiveTrack = MaterialTheme.colorScheme.secondaryContainer
    val onInactivePill = MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pageColor)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // MATCHED: The exact spacer height from SignUpContent
        Spacer(modifier = Modifier.height(50.dp))

        // MATCHED: Exact image size and description tags from SignUpContent
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(R.string.auth_logo_desc),
            modifier = Modifier.size(200.dp)
        )

        // MATCHED: Exact spacing gap under logo
        Spacer(modifier = Modifier.height(10.dp))

        // MATCHED: Exact card rounding token
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- PILL TOGGLE SWITCH ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .background(inactiveTrack, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Sign In Button (Active on this layout view)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(activePill, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_sign_in),
                            color = onActivePill,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    // Sign Up Button (Tapping this changes the screen route)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable(onClick = onNavigateToSignUp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.auth_sign_up),
                            color = onInactivePill,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }

                // --- FORM FIELDS ---
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text(stringResource(R.string.auth_email_hint)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text(stringResource(R.string.auth_password_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
                )

                // --- ERROR PRESENTATION STATE LAYER ---
                if (uiState is SignInUiState.Error) {
                    Text(
                        text = uiState.detail ?: stringResource(uiState.messageRes),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontSize = 13.sp
                    )
                }

                // --- ACTION SUBMIT BUTTON ---
                Button(
                    onClick = { onSignIn(email, password) },
                    enabled = uiState !is SignInUiState.Submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = activePill),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(
                        stringResource(R.string.auth_login_button),
                        color = onActivePill,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
@Preview(showBackground = true, name = "Light")
@Composable
private fun LoginContentPreview() {
    ShoppingMadeBetterTheme(darkTheme = false) {
        LoginContent(
            uiState = SignInUiState.Idle,
            onSignIn = { _, _ -> },
            onNavigateToSignUp = {},
        )
    }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun LoginContentDarkPreview() {
    ShoppingMadeBetterTheme(darkTheme = true) {
        LoginContent(
            uiState = SignInUiState.Idle,
            onSignIn = { _, _ -> },
            onNavigateToSignUp = {},
        )
    }
}
