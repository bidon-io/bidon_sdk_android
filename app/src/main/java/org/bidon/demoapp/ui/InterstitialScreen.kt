package org.bidon.demoapp.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.AppToolbar
import org.bidon.demoapp.component.Body1Text
import org.bidon.demoapp.component.Body2Text
import org.bidon.demoapp.ui.ext.getImpressionInfo
import org.bidon.demoapp.ui.ext.toJson
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.interstitial.InterstitialAd
import org.bidon.sdk.ads.interstitial.InterstitialListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo

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
        mutableStateOf("0.001")
    }

    val interstitial by lazy {
        InterstitialAd().apply {
            setInterstitialListener(
                object : InterstitialListener {
                    override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                        logFlow.log("onAdLoaded WINNER:\n$ad. AuctionInfo: \n${auctionInfo.toJson()}")
                        logFlow.log("onAdLoaded ImpressionInfo: \n${ad.getImpressionInfo()}")
                    }

                    override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                        logFlow.log("onAdLoadFailed: $cause: ${cause.message}. AuctionInfo: \n${auctionInfo?.toJson()}")
                    }

                    override fun onAdShowFailed(cause: BidonError) {
                        logFlow.log("onAdShowFailed: $cause: ${cause.message}")
                    }

                    override fun onAdShown(ad: Ad) {
                        logFlow.log("onAdShown: $ad")
                        logFlow.log("onAdShown ImpressionInfo: \n${ad.getImpressionInfo()}")
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

                    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                        logFlow.log("onRevenuePaid: ad=$ad, adValue=$adValue")
                        logFlow.log("onRevenuePaid ImpressionInfo: \n${ad.getImpressionInfo()}")
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppToolbar(
            title = "Interstitial Ad",
            onNavigationButtonClicked = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Body1Text(text = "Pricefloor $")
                BasicTextField(
                    value = pricefloorState.value,
                    onValueChange = { newValue ->
                        pricefloorState.value = newValue
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        background = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    maxLines = 1
                )
                AppTextButton(
                    text = "Add extras"
                ) {
                    interstitial.addExtra("some_extra_obj", interstitial)
                    interstitial.addExtra("some_extra_int", 123)
                    interstitial.addExtra("some_extra_data", "some_value")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AppButton(text = "Load") {
                    val pricefloor = pricefloorState.value.toDoubleOrNull()
                    if (pricefloor == null) {
                        pricefloorState.value = BidonSdk.DefaultPricefloor.toString()
                    }
                    interstitial.loadAd(activity, pricefloor = pricefloor ?: BidonSdk.DefaultPricefloor)
                }
                AppButton(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Show"
                ) {
                    interstitial.showAd(activity)
                }
                AppButton(
                    modifier = Modifier.padding(start = 8.dp),
                    text = "Destroy"
                ) {
                    interstitial.destroyAd()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AppTextButton(
                    modifier = Modifier.padding(start = 0.dp),
                    text = "Notify Loss"
                ) {
                    interstitial.notifyLoss(
                        winnerDemandId = "som_winner_demand",
                        winnerPrice = 234.567
                    )
                    logFlow.log("NotifyLoss")
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppTextButton(text = "Notify Win") {
                    interstitial.notifyWin()
                    logFlow.log("NotifyWin")
                }
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
                            .background(MaterialTheme.colorScheme.secondary, MaterialTheme.shapes.medium)
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
    logInfo(TAG, string)
}

private const val TAG = "InterstitialScreen"
