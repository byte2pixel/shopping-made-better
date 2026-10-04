package com.fullsail.shoppingmadebetter.feature.onboarding.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@Composable
fun GoalPreferenceScreen(
    selectedGoal: String?,
    onGoalSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val goals = listOf(
        "Save money & budget",
        "Eat healthier & meal plan",
        "Track pantry inventory & reduce waste",
        "Streamline shopping trips efficiently"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "What is your primary goal using this app?",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            goals.forEach { goal ->
                OptionCard(
                    label = goal,
                    isSelected = goal == selectedGoal,
                    onClick = { onGoalSelected(goal) },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GoalPreferenceScreenPreview() {
    ShoppingMadeBetterTheme {
        GoalPreferenceScreen(
            selectedGoal = "Save money & budget",
            onGoalSelected = {}
        )
    }
}
