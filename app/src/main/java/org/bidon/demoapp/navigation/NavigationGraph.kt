package org.bidon.demoapp.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.bidon.demoapp.ui.*
import org.bidon.demoapp.ui.settings.ServerSettingsScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    shared: SharedPreferences,
) {
    val initState = remember {
        mutableStateOf(MainScreenState.NotInitialized)
    }

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController, initState, shared)
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
            ServerSettingsScreen(navController, shared)
        }
    }
}
