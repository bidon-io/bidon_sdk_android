package com.appodeal.mads.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.appodeal.mads.ui.InterstitialScreen
import com.appodeal.mads.ui.MainScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
) {
    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController)
        }
        composable(Screen.Interstitial.route) {
            InterstitialScreen()
        }
        composable(Screen.Rewarded.route) {

        }
        composable(Screen.Banners.route) {

        }
    }
}
