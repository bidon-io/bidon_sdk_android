package com.appodeal.mads.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.AppToolbar
import com.appodeal.mads.component.Body2Text
import com.appodealstack.bidon.ad.Interstitial
import com.appodealstack.bidon.ad.InterstitialListener
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.auctions.data.models.AuctionResult

@Composable
fun InterstitialScreen(
    navController: NavHostController,
) {

    val activity = LocalContext.current as Activity
    val interstitial = Interstitial("c7c5f664e60b9bfb")//.BNMaxInterstitialAd("c7c5f664e60b9bfb", activity)

    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Max Interstitial",
            onNavigationButtonClicked = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            AppButton(text = "Load") {
                interstitial.setInterstitialListener(
                    object : InterstitialListener {
                        override fun onAdLoaded(ad: Ad) {
                            logFlow.log("onAdLoaded: $ad")
                        }

                        override fun onAdLoadFailed(cause: Throwable) {
                            logFlow.log("onAdLoadFailed: $cause")
                        }

                        override fun onAdShown(ad: Ad) {
                            logFlow.log("onAdShown: $ad")
                        }

                        override fun onAdShowFailed(cause: Throwable) {
                            logFlow.log("onAdShowFailed: $cause")
                        }

                        override fun onAdImpression(ad: Ad) {
                            logFlow.log("onAdImpression: $ad")
                        }

                        override fun onAdClicked(ad: Ad) {
                            logFlow.log("onAdClicked: $ad")
                        }

                        override fun onAdClosed(ad: Ad) {
                            logFlow.log("onAdClosed: $ad")
                        }

                        override fun auctionStarted() {
                            logFlow.log("auctionStarted")
                        }

                        override fun auctionSucceed(auctionResults: List<AuctionResult>) {
                            logFlow.log("auctionSucceed: $auctionResults")
                        }

                        override fun auctionFailed(error: Throwable) {
                            logFlow.log("auctionFailed: $error")
                        }

                        override fun roundStarted(roundId: String) {
                            logFlow.log("roundStarted: roundId=$roundId")
                        }

                        override fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {
                            logFlow.log("roundSucceed: roundId=$roundId. roundResults=$roundResults")
                        }

                        override fun roundFailed(roundId: String, error: Throwable) {
                            logFlow.log("roundFailed: roundId=$roundId, $error")
                        }
                    }
                )
                interstitial.load()
            }
            AppButton(text = "Show") {
                interstitial.show(activity)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
            ) {
                items(logFlow.value) { logLine ->
                    Column(
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .background(MaterialTheme.colors.secondary, MaterialTheme.shapes.medium)
                            .padding(4.dp)
                    ) {
                        Body2Text(text = logLine)
                    }
                }
            }
        }
    }
}

private fun MutableState<List<String>>.log(string: String) {
    synchronized(this) {
        this.value = this.value + string
    }
}