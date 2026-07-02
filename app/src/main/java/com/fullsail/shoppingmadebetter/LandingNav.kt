package com.fullsail.shoppingmadebetter


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

val pages = listOf(LandingPages("Login","login", {ShoppingLogin()}),
    LandingPages("Shopping List","shoppinglist", {ShoppingList()}),
    LandingPages("Cart", "cart", {ShoppingCart()}),
    LandingPages("Pantry","pantry", {ShoppingPantry()}),
    LandingPages("History", "history",{ ShoppingHistory()}))

@Composable
fun ShopNav() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "home"
    )
    {
        for (landingPage in pages) {
            composable(landingPage.route) {
                landingPage.page()
            }

            composable("home")
            {
                LandingPage(navController)
            }

        }
    }
}


@Composable
fun ShoppingList() {
    Text("This is the shopping list")
}
@Composable
fun ShoppingLogin() {
    Text("This is the login")
}
@Composable
fun ShoppingCart() {
    Text("This is the shopping cart")
}
@Composable
fun ShoppingPantry() {
    Text("This is the pantry")
}
@Composable
fun ShoppingHistory() {
    Text("This is the shopping history")
}

@Composable
fun LandingPage(navController: NavController)
{

    LazyVerticalGrid( columns = GridCells.Adaptive(minSize = 128.dp),
                      modifier = Modifier
                          .fillMaxSize()
                          .wrapContentSize(Alignment.Center))
    {


        items(pages){ item ->
            Button(
                onClick = { navController.navigate(item.route) }
            )
            {
                Text(item.name)
            }

        }

    }
}

