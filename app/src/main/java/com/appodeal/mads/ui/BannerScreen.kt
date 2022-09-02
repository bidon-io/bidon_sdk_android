package com.appodeal.mads.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
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
import kotlin.math.max
import kotlin.math.min

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
    val showOnLoad = remember {
        mutableStateOf(false)
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp)
                .dashedBorder(
                    width = 1.dp,
                    radius = 0.dp,
                    color = MaterialTheme.colors.error
                )
                .padding(0.dp),
            contentAlignment = Alignment.Center
        ) {
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
                        view
                    }
                )
            } else {
                Subtitle1Text(text = "Place for Banner", modifier = Modifier.padding(8.dp))
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            ItemSelector(
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
            Spacer(modifier = Modifier.padding(top = 2.dp))

            val autoRefreshText = "AutoRefresh " + if (autoRefreshTtl.value / 1000 == 0L) {
                "Off"
            } else {
                "each ${autoRefreshTtl.value / 1000} sec."
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberScroller(
                    modifier = Modifier.weight(1f),
                    initialValue = DefaultAutoRefreshTimeoutMs / 1000f,
                    value = (autoRefreshTtl.value.toInt() / 1000).toString(),
                    onValueChanges = {
                        val newTimeout = min(
                            a = max(
                                a = (it * 1000L).toLong(),
                                b = 0L
                            ),
                            b = 30000L
                        )
                        if (newTimeout == 0L) {
                            bannerView.value?.stopAutoRefresh()
                        } else {
                            bannerView.value?.startAutoRefresh(timeoutMs = newTimeout)
                        }
                        autoRefreshTtl.value = newTimeout
                    },
                    onPlusClicked = {
                        val newTimeout = min(autoRefreshTtl.value + 5000, 30_000L)
                        bannerView.value?.startAutoRefresh(timeoutMs = newTimeout)
                        autoRefreshTtl.value = newTimeout
                    },
                    onMinusClicked = {
                        val newTimeout = max(autoRefreshTtl.value - 5000, 0L)
                        if (newTimeout == 0L) {
                            bannerView.value?.stopAutoRefresh()
                        } else {
                            bannerView.value?.startAutoRefresh(timeoutMs = newTimeout)
                        }
                        autoRefreshTtl.value = newTimeout
                    }
                )
                Body2Text(
                    modifier = Modifier.weight(2f),
                    text = autoRefreshText
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppButton(text = "Create") {
                    bannerView.value = BannerView(
                        context = context,
                        placementId = "some_placement_id"
                    ).apply {
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
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(
                    text = "Load",
                ) {
                    bannerView.value?.load()
                    if (showOnLoad.value) {
                        bannerView.value?.show()
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Body2Text(text = "Show onLoad")
                Checkbox(
                    colors = CheckboxDefaults.colors(MaterialTheme.colors.onBackground),
                    checked = showOnLoad.value, onCheckedChange = {
                        showOnLoad.value = it
                    }
                )
            }
            Row {
                AppButton(text = "Show") {
                    bannerView.value?.show()
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(text = "Destroy") {
                    bannerView.value?.destroy()
                    bannerView.value = null
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
