package com.fullsail.shoppingmadebetter.feature.shoppinglists.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage

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
                Column()
                {

                    AsyncImage(model = state.item.id.image, modifier = Modifier.height(100.dp).fillMaxWidth(),  contentDescription = state.item.id.title)
                    state.item.id.title?.let { Text(it, style = MaterialTheme.typography.headlineLarge) }
                   Row()
                   {
                       state.item.id.brand?.let { Text(it,style = MaterialTheme.typography.labelLarge) }
                       Spacer(modifier = Modifier.padding(12.dp))
                       state.item.id.lifeCategory?.let { Text(it,style = MaterialTheme.typography.labelLarge) }
                   }
                    HorizontalDivider()
                    state.item.id.description?.let { Text(it,style = MaterialTheme.typography.bodyLarge) }
                }

            }


            else -> {}
        }
    }
}


