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
    val initMediation = remember {
        mutableStateOf(MediationSdk.None)
    }
    val initState = remember {
        mutableStateOf(false)
    }
    val interstitialViewModel = InterstitialViewModel()
    val rewardedViewModel = RewardedViewModel()
    val bannerApplovinViewModel = BannerApplovinViewModel()
    val bannerFyberBanner = BannerFyberViewModel()

    NavHost(navController = navController, startDestination = Screen.Main.route) {
        composable(Screen.Main.route) {
            MainScreen(navController, initState, initMediation)
        }
        composable(Screen.Interstitial.route) {
            InterstitialScreen(navController, interstitialViewModel, initMediation.value)
        }
        composable(Screen.Rewarded.route) {
            RewardedScreen(navController, rewardedViewModel, initMediation.value)
        }
        composable(Screen.Banners.route) {
            when(initMediation.value){
                MediationSdk.None -> {
                }
                MediationSdk.Applovin -> BannerApplovinScreen(navController, bannerApplovinViewModel)
                MediationSdk.Fyber -> BannerFyberScreen(navController, bannerFyberBanner)
            }
        }
    }
}
