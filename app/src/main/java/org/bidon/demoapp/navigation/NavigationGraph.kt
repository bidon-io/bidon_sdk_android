package org.bidon.demoapp.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.bidon.demoapp.ui.*
import org.bidon.demoapp.ui.settings.ServerSettingsScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
) {
    val initState = remember {
        mutableStateOf(MainScreenState.NotInitialized)
    }
    val shared = LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE)

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
        composable(Screen.PositionedBanners.route) {
            PositionedBannerScreen(
                navController = navController,
                viewModel = viewModel()
            )
        }
        composable(Screen.ServerSettings.route) {
            ServerSettingsScreen(navController, shared)
        }
    }
}
