package org.bidon.demoapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.bidon.demoapp.component.*
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.banner.Banner
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo

@Composable
fun BannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }
    val bannerFormat = remember {
        mutableStateOf(BannerFormat.Banner)
    }
    val showOnLoad = remember {
        mutableStateOf(false)
    }
    val banner = remember {
        mutableStateOf<Banner?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AppToolbar(
            title = "Banners",
            onNavigationButtonClicked = { navController.popBackStack() }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .dashedBorder(
                    width = 1.dp,
                    radius = 0.dp,
                    color = MaterialTheme.colorScheme.error
                )
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
            val view = banner.value
            if (view != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth(),
//                    .height(
//                        when (bannerSize.value) {
//                            BannerSize.Banner -> 50.dp
//                            BannerSize.LeaderBoard -> 90.dp
//                            BannerSize.MRec -> 250.dp
//                            BannerSize.Large -> 100.dp
//                            BannerSize.Adaptive -> 100.dp
//                        }
//                    ), // TODO Admob.OnPaidListener isn't invoked using ComposeView, but always in XML-Layout. Check it.
                    factory = {
                        view
                    }
                )
            } else {
                Subtitle1Text(text = "Place for Banner", modifier = Modifier.padding(8.dp))
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            ItemSelector(
                items = BannerFormat.values().toList(),
                selectedItem = bannerFormat.value,
                getItemTitle = {
                    when (it) {
                        BannerFormat.Banner -> "Banner 320x50"
                        BannerFormat.LeaderBoard -> "Leader Board 728x90"
                        BannerFormat.MRec -> "MRec 300x250"
                        BannerFormat.Adaptive -> "Smart/Adaptive 320x50"
                    }
                },
                onItemClicked = {
                    bannerFormat.value = it
                    banner.value?.setBannerFormat(it)
                }
            )
            Spacer(modifier = Modifier.padding(top = 2.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppButton(text = "Create") {
                    banner.value = Banner(
                        context = context,
                        placementId = "some_placement_id"
                    ).apply {
                        setBannerFormat(bannerFormat.value)
                        setBannerListener(
                            object : BannerListener {
                                override fun onAdLoaded(ad: Ad) {
                                    logFlow.log("onAdLoaded WINNER:\n$ad")
                                }

                                override fun onAdLoadFailed(cause: BidonError) {
                                    logFlow.log("onAdLoadFailed: $cause")
                                }

                                override fun onAdShown(ad: Ad) {
                                    logFlow.log("onAdShown: $ad")
                                }

                                override fun onAdClicked(ad: Ad) {
                                    logFlow.log("onAdClicked: $ad")
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

                                override fun onAuctionFailed(cause: BidonError) {
                                    logFlow.log("auctionFailed: $cause")
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

                                override fun onRoundFailed(roundId: String, cause: BidonError) {
                                    logFlow.log("roundFailed: roundId=$roundId, $cause")
                                }

                                override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                                    logFlow.log("onRevenuePaid: ad=$ad, adValue=$adValue")
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(
                    text = "Load",
                ) {
                    banner.value?.loadAd()
                    if (showOnLoad.value) {
                        banner.value?.showAd()
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Body2Text(text = "Show onLoad")
                Checkbox(
                    colors = CheckboxDefaults.colors(MaterialTheme.colorScheme.onBackground),
                    checked = showOnLoad.value, onCheckedChange = {
                        showOnLoad.value = it
                    }
                )
            }
            Row {
                AppButton(text = "Show") {
                    banner.value?.showAd()
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(text = "Destroy") {
                    banner.value?.destroyAd()
                    banner.value = null
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
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

fun Modifier.dashedBorder(width: Dp, radius: Dp, color: Color) =
    drawBehind {
        drawIntoCanvas {
            val paint = Paint()
                .apply {
                    strokeWidth = width.toPx()
                    this.color = color
                    style = PaintingStyle.Stroke
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
            it.drawRoundRect(
                width.toPx(),
                width.toPx(),
                size.width - width.toPx(),
                size.height - width.toPx(),
                radius.toPx(),
                radius.toPx(),
                paint
            )
        }
    }

private fun MutableState<List<String>>.log(string: String) {
    synchronized(this) {
        this.value = this.value + string
    }
    logInfo(Tag, string)
}

private const val Tag = "BannerScreen"
private const val DefaultAutoRefreshTimeoutMs = 10000L