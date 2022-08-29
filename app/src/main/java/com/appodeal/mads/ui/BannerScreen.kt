package com.appodeal.mads.ui

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
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
import com.appodeal.mads.component.*
import com.appodealstack.bidon.ad.BannerListener
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.view.BannerView
import com.appodealstack.bidon.view.DefaultAutoRefreshTimeoutMs
import kotlinx.coroutines.launch

@Composable
fun BannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }
    val bannerSize = remember {
        mutableStateOf(BannerSize.Banner)
    }
    val autoRefreshTtl = remember {
        mutableStateOf(DefaultAutoRefreshTimeoutMs)
    }
    val bannerView = remember {
        mutableStateOf<BannerView?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Banners",
            onNavigationButtonClicked = { navController.popBackStack() }
        )

        val view = bannerView.value
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
                    view.apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .dashedBorder(width = 1.dp, radius = 4.dp, color = MaterialTheme.colors.error)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Subtitle1Text(text = "Place for Banner")
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            state = listState
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
                            BannerSize.Adaptive -> "Smart/Adaptive"
                        }
                    },
                    onItemClicked = {
                        bannerSize.value = it
                        bannerView.value?.setAdSize(it)
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
                        val newTimeout = ((it * 1000).toLong())
                        if (newTimeout == 0L) {
                            bannerView.value?.stopAutoRefresh()
                        } else {
                            bannerView.value?.startAutoRefresh(timeoutMs = newTimeout)
                        }
                        autoRefreshTtl.value = newTimeout
                    },
                    steps = 30,
                    valueRange = 0f..30f
                )

                AppButton(text = "Create banner") {
                    bannerView.value = BannerView(context, "some_placement_id").apply {
                        setAdSize(bannerSize.value)
                        if (autoRefreshTtl.value == 0L) {
                            stopAutoRefresh()
                        } else {
                            startAutoRefresh(timeoutMs = autoRefreshTtl.value)
                        }
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
                }
                if (bannerView.value != null) {
                    AppButton(text = "Load") {
                        bannerView.value?.load()
                    }
                    AppButton(text = "Destroy") {
                        bannerView.value?.destroy()
                        bannerView.value = null
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
    logInternal(Tag, string)
}

private const val Tag = "BannerScreen"
