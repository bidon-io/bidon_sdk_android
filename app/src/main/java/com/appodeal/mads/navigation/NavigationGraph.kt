package com.appodeal.mads.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.appodeal.mads.ui.*

@Composable
fun NavigationGraph(
    navController: NavHostController,
) {
    val initState = remember {
        mutableStateOf(false)
    }
    val interstitialViewModel = InterstitialViewModel()
    val rewardedViewModel = RewardedViewModel()
    val bannerViewModel = BannerViewModel()

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController, initState)
        }
        composable(Screen.Interstitial.route) {
            InterstitialScreen(navController, interstitialViewModel)
        }
        composable(Screen.Rewarded.route) {
            RewardedScreen(navController, rewardedViewModel)
        }
        composable(Screen.Banners.route) {
            BannerScreen(navController, bannerViewModel)
        }
    }
}
