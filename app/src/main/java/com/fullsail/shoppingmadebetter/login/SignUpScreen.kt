package com.fullsail.shoppingmadebetter.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.gotrue.auth

@Composable
fun SignUpScreen(supabaseClient: io.github.jan.supabase.SupabaseClient? = null) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()


    val lightGreenBg = Color(0xFFC2F0C2)
    val darkGreenActive = Color(0xFF4A5D4E)
    val lightGreenInactive = Color(0xFFD5E8D4)
    val whiteCardBg = Color(0xFFF7F9F6)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(lightGreenBg)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))


        Image(
            painter = painterResource(id = com.fullsail.shoppingmadebetter.R.drawable.app_logo),
            contentDescription = "Shopping Made Better Logo",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))


        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = whiteCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .background(lightGreenInactive, shape = RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable {
                                // This will link directly to Mel's Login screen path later
                                println("Navigation Link: Route user back to Sign In Screen")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            color = darkGreenActive,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }


                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(darkGreenActive, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign Up",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // 📥 INPUT REGISTRATION FIELDS
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    placeholder = { Text("Enter email", color = Color.Gray) },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = whiteCardBg,
                        unfocusedContainerColor = whiteCardBg
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    placeholder = { Text("Password", color = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = com.fullsail.shoppingmadebetter.R.drawable.ic_favorite), // Temporary fallback visibility eyeball indicator
                            contentDescription = "Toggle Visibility",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = whiteCardBg,
                        unfocusedContainerColor = whiteCardBg
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = "" },
                    placeholder = { Text("Confirm Password", color = Color.Gray) },
                    visualTransformation = PasswordVisualTransformation(),
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = com.fullsail.shoppingmadebetter.R.drawable.ic_favorite),
                            contentDescription = "Toggle Visibility",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Gray,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = whiteCardBg,
                        unfocusedContainerColor = whiteCardBg
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontSize = 13.sp
                    )
                }

                if (isSuccess) {
                    Text(
                        text = "Account created successfully!",
                        color = darkGreenActive,
                        modifier = Modifier.padding(bottom = 16.dp),
                        fontSize = 13.sp
                    )
                }

                // 🚀 SUBMIT BUTTON
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            errorMessage = "Error: All registration forms must be complete."
                        } else if (password != confirmPassword) {
                            errorMessage = "Error: Passwords do not match."
                        } else {
                            scope.launch {
                                try {
                                    if (supabaseClient != null) {
                                        val inputEmail = email
                                        val inputPassword = password

                                        supabaseClient.auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                                            this.email = inputEmail
                                            this.password = inputPassword
                                        }
                                        isSuccess = true
                                        errorMessage = ""
                                    } else {
                                        println("Database Client Missing: Local Mock Sign Up successful for $email")
                                        isSuccess = true
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Registration connection failed."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = darkGreenActive),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Sign Up", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}