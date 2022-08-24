package com.appodeal.mads.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.appodeal.mads.ui.*
import com.appodeal.mads.ui.settings.ServerSettingsScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
) {
    val initState = remember {
        mutableStateOf(MainScreenState.NotInitialized)
    }

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController, initState)
        }
        composable(Screen.Interstitial.route) {
            InterstitialScreen(navController)
        }
        composable(Screen.Rewarded.route) {
            RewardedScreen(navController)
        }
        composable(Screen.Banners.route) {
            BannerScreen(navController)
        }
        composable(Screen.ServerSettings.route) {
            ServerSettingsScreen(navController)
        }
    }
}
