package com.fullsail.shoppingmadebetter.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fullsail.shoppingmadebetter.ui.theme.ShoppingMadeBetterTheme

@Composable
fun MealDetailsScreen(
    mealId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Recipe Image Placeholder", color = Color.DarkGray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Loading recipe for Meal: $mealId",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Instructions and ingredients will populate here.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailsScreenPreview() {
    ShoppingMadeBetterTheme {
        MealDetailsScreen(
            mealId = "12345",
            onNavigateBack = {}
        )
    }
}