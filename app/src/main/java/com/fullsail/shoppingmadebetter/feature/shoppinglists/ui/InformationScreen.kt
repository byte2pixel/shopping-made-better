package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun InformationScreen(
    modifier: Modifier = Modifier,
    viewModel: InformationViewModel = hiltViewModel(),
    productId : String,
) {
    val uiState by viewModel.uiState.collectAsState()
    Box(Modifier.fillMaxSize()) {
        LaunchedEffect(productId) { viewModel.getItem(productId) }
        when (val state = uiState) {
            ShoppingTripsUiState.Loading ->
                CircularProgressIndicator(Modifier.align(Alignment.Center))

            ShoppingTripsUiState.Error ->
                Text("Couldn't load your lists", Modifier.align(Alignment.Center))

            is ItemInformationState.Success ->{
                state.item.id.description?.let { Text(it) }
            }


            else -> {}
        }
    }
}


