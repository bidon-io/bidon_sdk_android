package com.appodeal.mads.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.H5Text
import com.appodeal.mads.navigation.Screen
import com.appodealstack.admob.AdmobAdapter
import com.appodealstack.admob.AdmobParameters
import com.appodealstack.applovin.AppLovinDecorator
import com.appodealstack.bidmachine.BidMachineAdapter
import com.appodealstack.bidmachine.BidMachineParameters
import com.appodealstack.fyber.FairBidDecorator

enum class MediationSdk {
    None,
    Applovin,
    Fyber
}

@Composable
fun MainScreen(
    navController: NavHostController,
    initState: MutableState<Boolean>,
    initMediation: MutableState<MediationSdk>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current
        if (!initState.value) {
            AppButton(text = "Init Applovin+BidOn SDK") {
                initApplovinSdk(context,
                    onInitialized = {
                        initState.value = true
                        initMediation.value = MediationSdk.Applovin
                    })
            }
            AppButton(text = "Init Fyber+BidOn SDK") {
                initFyberSdk(context,
                    onInitialized = {
                        initState.value = true
                        initMediation.value = MediationSdk.Fyber
                    })
            }
        } else {

            H5Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text = when (initMediation.value) {
                    MediationSdk.None -> ""
                    MediationSdk.Applovin -> "Applovin MAX"
                    MediationSdk.Fyber -> "Fyber FairBid"
                }
            )
            AppButton(text = "Interstitial") {
                navController.navigate(Screen.Interstitial.route)
            }
            AppButton(text = "Rewarded") {
                navController.navigate(Screen.Rewarded.route)
            }
            AppButton(text = "Banner") {
                navController.navigate(Screen.Banners.route)
            }
        }
    }
}

private fun initApplovinSdk(
    context: Context,
    onInitialized: () -> Unit
) {
    AppLovinDecorator.getInstance(context).mediationProvider = "max"
    AppLovinDecorator
        .register(
            AdmobAdapter::class.java,
            AdmobParameters(
                interstitials = mapOf(
                    0.1 to "ca-app-pub-3940256099942544/1033173712",
                    1.0 to "ca-app-pub-3940256099942544/1033173712",
                    2.0 to "ca-app-pub-3940256099942544/1033173712",
                ),
                rewarded = mapOf(
                    0.1 to "ca-app-pub-3940256099942544/5224354917",
                    1.0 to "ca-app-pub-3940256099942544/5224354917",
                    2.0 to "ca-app-pub-3940256099942544/5224354917",
                ),
                banners = mapOf(
                    0.1 to "ca-app-pub-3940256099942544/6300978111",
                    1.0 to "ca-app-pub-3940256099942544/6300978111",
                    2.0 to "ca-app-pub-3940256099942544/6300978111",
                ),
            )
        )
        .register(
            BidMachineAdapter::class.java,
            BidMachineParameters(sourceId = "1")
        )
        .initializeSdk(context) { appLovinSdkConfiguration ->
            onInitialized()
        }
}


private fun initFyberSdk(
    context: Context,
    onInitialized: () -> Unit
) {
    FairBidDecorator
//        .register(
//            AdmobAdapter::class.java,
//            AdmobParameters(
//                interstitials = mapOf(
//                    0.1 to "ca-app-pub-3940256099942544/1033173712",
//                    1.0 to "ca-app-pub-3940256099942544/1033173712",
//                    2.0 to "ca-app-pub-3940256099942544/1033173712",
//                ),
//                rewarded = mapOf(
//                    0.1 to "ca-app-pub-3940256099942544/5224354917",
//                    1.0 to "ca-app-pub-3940256099942544/5224354917",
//                    2.0 to "ca-app-pub-3940256099942544/5224354917",
//                ),
//                banners = mapOf(
//                    0.1 to "ca-app-pub-3940256099942544/6300978111",
//                    1.0 to "ca-app-pub-3940256099942544/6300978111",
//                    2.0 to "ca-app-pub-3940256099942544/6300978111",
//                ),
//            )
//        )
//        .register(
//            BidMachineAdapter::class.java,
//            BidMachineParameters(sourceId = "1")
//        )
        .start(
            appId = "109613",
            activity = context as Activity,
            onInitialized = {
                onInitialized.invoke()
            }
        )
}