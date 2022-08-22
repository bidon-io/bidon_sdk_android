package com.appodeal.mads.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.appodeal.mads.component.*
import com.appodealstack.bidon.ad.Banner
import com.appodealstack.bidon.ad.BannerListener
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.core.ext.logInternal

@Composable
fun BannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }
    val bannerSize = remember {
        mutableStateOf(BannerSize.Banner)
    }
    val autoRefreshTtl = remember {
        mutableStateOf(15000L)
    }
    val banner = remember {
        mutableStateOf<Banner?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Max Banner",
            onNavigationButtonClicked = { navController.popBackStack() }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .dashedBorder(width = 1.dp, radius = 4.dp, color = MaterialTheme.colors.error)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bannerView = banner.value
            if (bannerView != null) {
                AndroidView(
                    modifier = Modifier.height(
                        when (bannerSize.value) {
                            BannerSize.Banner -> 50.dp
                            BannerSize.LeaderBoard -> 90.dp
                            BannerSize.MRec -> 250.dp
                            BannerSize.Large -> 100.dp
                            BannerSize.Smart -> TODO()
                        }
                    ),
                    factory = {
                        bannerView
                    }
                )
            } else {
                Subtitle1Text(text = "Place for Banner")
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item {
                ItemSelector(
                    title = "Ad Format",
                    items = BannerSize.values().toList(),
                    selectedItem = bannerSize.value,
                    getItemTitle = {
                        when (it) {
                            BannerSize.Banner -> "Banner 320x50"
                            BannerSize.LeaderBoard -> "Leader Board 728x90"
                            BannerSize.MRec -> "MRec 300x250"
                            BannerSize.Large -> "Large 320x100"
                            BannerSize.Smart -> "Smart/Adaptive"
                        }
                    },
                    onItemClicked = {
                        bannerSize.value = it
                        banner.value?.setAdSize(it)
                    }
                )
                Spacer(modifier = Modifier.padding(top = 16.dp))

                val autoRefreshText = "AutoRefresh " + if (autoRefreshTtl.value / 1000 == 0L) {
                    "Off"
                } else {
                    "each ${autoRefreshTtl.value / 1000} sec."
                }
                Body2Text(text = autoRefreshText)
                Slider(
                    value = (autoRefreshTtl.value / 1000).toFloat(),
                    onValueChange = {
                        autoRefreshTtl.value = ((it * 1000).toLong())
                    },
                    steps = 30,
                    valueRange = 0f..30f
                )

                AppButton(text = "Create banner") {
                    banner.value = Banner(context, "some_placement_id").apply {
                        setBannerListener(
                            object : BannerListener {
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
                    banner.value?.setAdSize(bannerSize.value)
                }
                if (banner.value != null) {
                    AppButton(text = "Load") {
                        banner.value?.load()
                    }
                    AppButton(text = "Destroy") {
                        banner.value?.destroy()
                        banner.value = null
                    }
                }
            }
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
    logInternal(Tag, string)
}

private const val Tag = "BannerScreen"
