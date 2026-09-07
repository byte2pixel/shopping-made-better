package com.fullsail.shoppingmadebetter.feature.meals.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CreateRecipeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateRecipeViewModel = hiltViewModel()
) {
    val title by viewModel.title.collectAsState()
    val category by viewModel.category.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Recipe Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = category,
            onValueChange = viewModel::updateCategory,
            label = { Text("Category (e.g., Can Make)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ingredients,
            onValueChange = viewModel::updateIngredients,
            label = { Text("Ingredients (comma separated)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Button(
            onClick = {
                viewModel.saveRecipe()
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Recipe")
        }
    }
}