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
import com.appodealstack.bidon.core.ext.logInternal

@Composable
fun InterstitialScreen(
    navController: NavHostController,
) {

    val activity = LocalContext.current as Activity

    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }

    val interstitial by lazy {
        Interstitial("some_placement_id").apply {
            setInterstitialListener(
                object : InterstitialListener {
                    override fun onAdLoaded(ad: Ad) {
                        logFlow.log("onAdLoaded WINNER:\n$ad")
                    }

                    override fun onAdLoadFailed(cause: Throwable) {
                        logFlow.log("onAdLoadFailed: $cause")
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

                    override fun onAdExpired(ad: Ad) {
                        logFlow.log("onAdExpired: $ad")
                    }

                    override fun auctionStarted() {
                        logFlow.log("auctionStarted")
                    }

                    override fun auctionSucceed(auctionResults: List<AuctionResult>) {
                        val log = buildString {
                            appendLine("AuctionSucceed (${auctionResults.size} items)")
                            auctionResults.forEachIndexed { index, auctionResult ->
                                appendLine("#$index ${auctionResult.adSource.demandId.demandId} ${auctionResult.priceFloor}")
                            }
                        }
                        logFlow.log(log)
                    }

                    override fun auctionFailed(error: Throwable) {
                        logFlow.log("auctionFailed: $error")
                    }

                    override fun roundStarted(roundId: String) {
                        logFlow.log("RoundStarted(roundId=$roundId)")
                    }

                    override fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {
                        logFlow.log(
                            buildString {
                                appendLine("roundSucceed($roundId)")
                                roundResults.forEachIndexed { index, auctionResult ->
                                    appendLine("#$index ${auctionResult.adSource.demandId.demandId} ${auctionResult.priceFloor}")
                                }
                            }
                        )
                    }

                    override fun roundFailed(roundId: String, error: Throwable) {
                        logFlow.log("roundFailed: roundId=$roundId, $error")
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Interstitial Ad",
            onNavigationButtonClicked = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp)
        ) {
            AppButton(text = "Load") {
                interstitial.load()
            }
            AppButton(text = "Show") {
                interstitial.show(activity)
            }
            AppButton(text = "Destroy") {
                interstitial.destroy()
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
    logInternal(Tag, string)
}

private const val Tag = "InterstitialScreen"