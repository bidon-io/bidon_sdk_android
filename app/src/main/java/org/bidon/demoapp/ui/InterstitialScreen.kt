package org.bidon.demoapp.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppToolbar
import org.bidon.demoapp.component.Body1Text
import org.bidon.demoapp.component.Body2Text
import org.bidon.sdk.BidOnSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.interstitial.Interstitial
import org.bidon.sdk.ads.interstitial.InterstitialListener
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import kotlinx.coroutines.launch

@Composable
fun InterstitialScreen(
    navController: NavHostController,
) {
    val activity = LocalContext.current as Activity
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }
    val pricefloorState = remember {
        mutableStateOf("0.01")
    }

    val interstitial by lazy {
        Interstitial("some_placement_id").apply {
            setInterstitialListener(
                object : InterstitialListener {
                    override fun onAdLoaded(ad: Ad) {
                        logFlow.log("onAdLoaded WINNER:\n$ad")
                    }

                    override fun onAdLoadFailed(cause: BidonError) {
                        logFlow.log("onAdLoadFailed: $cause")
                    }

                    override fun onAdShowFailed(cause: BidonError) {
                        logFlow.log("onAdShowFailed: $cause")
                    }

                    override fun onAdShown(ad: Ad) {
                        logFlow.log("onAdShown: $ad")
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

                    override fun onAuctionStarted() {
                        logFlow.log("auctionStarted")
                    }

                    override fun onAuctionSuccess(auctionResults: List<AuctionResult>) {
                        val log = buildString {
                            appendLine("AuctionSucceed (${auctionResults.size} items)")
                            auctionResults.forEachIndexed { index, auctionResult ->
                                appendLine("#$index ${auctionResult.adSource.demandId.demandId} ${auctionResult.ecpm}")
                            }
                        }
                        logFlow.log(log)
                    }

                    override fun onAuctionFailed(error: Throwable) {
                        logFlow.log("auctionFailed: $error")
                    }

                    override fun onRoundStarted(roundId: String, pricefloor: Double) {
                        logFlow.log("RoundStarted(roundId=$roundId, pricefloor=$pricefloor)")
                    }

                    override fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {
                        logFlow.log(
                            buildString {
                                appendLine("roundSucceed($roundId)")
                                roundResults.forEachIndexed { index, auctionResult ->
                                    appendLine("#$index ${auctionResult.adSource.demandId.demandId} ${auctionResult.ecpm}")
                                }
                            }
                        )
                    }

                    override fun onRoundFailed(roundId: String, error: Throwable) {
                        logFlow.log("roundFailed: roundId=$roundId, $error")
                    }

                    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                        logFlow.log("onRevenuePaid: ad=$ad, adValue=$adValue")
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppButton(text = "Load") {
                    val pricefloor = pricefloorState.value.toDoubleOrNull()
                    if (pricefloor == null) {
                        pricefloorState.value = BidOnSdk.DefaultPricefloor.toString()
                    }
                    interstitial.loadAd(activity, pricefloor = pricefloor ?: BidOnSdk.DefaultPricefloor)
                }
                Body1Text(
                    text = "Pricefloor $", modifier = Modifier.padding(start = 16.dp)
                )
                BasicTextField(
                    value = pricefloorState.value,
                    onValueChange = { newValue ->
                        pricefloorState.value = newValue
                    },
                    textStyle = MaterialTheme.typography.body1.copy(
                        color = MaterialTheme.colors.onPrimary,
                        background = MaterialTheme.colors.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    maxLines = 1
                )
            }
            AppButton(text = "Show") {
                interstitial.showAd(activity)
            }
            AppButton(text = "Destroy") {
                interstitial.destroyAd()
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp),
                state = listState
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
                coroutineScope.launch {
                    listState.animateScrollToItem(index = logFlow.value.lastIndex)
                }
            }
        }
    }
}

private fun MutableState<List<String>>.log(string: String) {
    synchronized(this) {
        this.value = this.value + string
    }
    logInfo(Tag, string)
}

private const val Tag = "InterstitialScreen"