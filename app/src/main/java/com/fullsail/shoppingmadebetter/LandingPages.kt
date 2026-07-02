package com.fullsail.shoppingmadebetter

import androidx.compose.runtime.Composable

data class LandingPages(val name: String, val route: String, val page: @Composable () -> Unit)