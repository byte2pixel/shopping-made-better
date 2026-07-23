package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    isSubmitting: Boolean,
    selectedDiets: Set<String>,
    selectedCategories: Set<String>,
    selectedGoal: String?,
    onDietToggled: (String) -> Unit,
    onCategoryToggled: (String) -> Unit,
    onGoalSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Setup (${currentStep + 1}/3)") },
                navigationIcon = {
                    TextButton(
                        onClick = {
                            if (currentStep > 0) {
                                currentStep--
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Back", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (currentStep < 2) {
                        Button(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = onSubmit,
                            enabled = !isSubmitting && selectedGoal != null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Complete Setup")
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentStep,
                label = "OnboardingStepAnimation"
            ) { step ->
                when (step) {
                    0 -> DietaryPreferenceScreen(
                        selectedDiets = selectedDiets,
                        onDietToggled = onDietToggled
                    )
                    1 -> CategoryPreferenceScreen(
                        selectedCategories = selectedCategories,
                        onCategoryToggled = onCategoryToggled
                    )
                    2 -> GoalPreferenceScreen(
                        selectedGoal = selectedGoal,
                        onGoalSelected = onGoalSelected
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun OnboardingScreenPreview() {
    ShoppingMadeBetterTheme {
        OnboardingScreen(
            isSubmitting = false,
            selectedDiets = setOf("Vegan"),
            selectedCategories = setOf("🥦 Produce"),
            selectedGoal = "Save money & budget",
            onDietToggled = {},
            onCategoryToggled = {},
            onGoalSelected = {},
            onSubmit = {},
            onNavigateBack = {}
        )
    }
}