package org.bidon.demoapp.ui

import android.app.Activity
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
import org.bidon.demoapp.ui.ext.getImpressionInfo
import org.bidon.demoapp.ui.ext.toJson
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.ads.banner.BannerView
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo

@Composable
fun BannerScreen(navController: NavHostController) {
//    ServerlessAuctionConfig.setLocalAuctionResponse(
//        rounds = listOf(
//            Round(
//                id = "ROUND_1",
//                demandIds = listOf("admob"),
//                timeoutMs = 10000
//            )
//        ),
//        lineItems = listOf(
//            LineItem(
//                demandId = "admob",
//                pricefloor = 0.01,
//                adUnitId = "ca-app-pub-3940256099942544/6300978111"
//            )
//        ),
//        pricefloor = 0.0
//    )
    val activity = LocalContext.current as Activity
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }
    val bannerFormat = remember {
        mutableStateOf(BannerFormat.Adaptive)
    }
    val showOnLoad = remember {
        mutableStateOf(false)
    }
    val bannerExists = remember {
        mutableStateOf(false)
    }
    var bannerView: BannerView? = null

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
            Subtitle1Text(text = "Place for Banner", modifier = Modifier.padding(8.dp))
            if (bannerExists.value) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 50.dp),
                    factory = { context ->
                        logInfo(TAG, "AndroidView factory")
                        BannerView(
                            context = context,
                        ).apply {
                            setBannerFormat(bannerFormat.value)
                            setBannerListener(
                                object : BannerListener {
                                    override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                                        logFlow.log("onAdLoaded WINNER:\n$ad. AuctionInfo: \n${auctionInfo.toJson()}")
                                        if (showOnLoad.value) {
                                            bannerView?.showAd()
                                        }
                                        logFlow.log("onAdLoaded ImpressionInfo: \n${ad.getImpressionInfo()}")
                                    }

                                    override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                                        logFlow.log("onAdLoadFailed: $cause. AuctionInfo: \n${auctionInfo?.toJson()}")
                                    }

                                    override fun onAdShown(ad: Ad) {
                                        logFlow.log("onAdShown: $ad")
                                        logFlow.log("onAdShown ImpressionInfo: \n${ad.getImpressionInfo()}")
                                    }

                                    override fun onAdClicked(ad: Ad) {
                                        logFlow.log("onAdClicked: $ad")
                                    }

                                    override fun onAdExpired(ad: Ad) {
                                        logFlow.log("onAdExpired: $ad")
                                    }

                                    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                                        logFlow.log("onRevenuePaid: ad=$ad, adValue=$adValue")
                                        logFlow.log("onRevenuePaid ImpressionInfo: \n${ad.getImpressionInfo()}")
                                    }

                                    override fun onAdShowFailed(cause: BidonError) {
                                        logFlow.log("onAdShowFailed: $cause")
                                    }
                                }
                            )
                        }.also {
                            bannerView = it
                            logFlow.log("New BannerView created: $it")
                        }
                    },
                    update = {
                        logInfo(TAG, "AndroidView update: $it")
                    }
                )
            }
        }
        Column(modifier = Modifier.padding(8.dp)) {
            ItemSelector(
                items = BannerFormat.values().toList(),
                selectedItem = bannerFormat.value,
                horizontalAlignment = Alignment.Start,
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
                    bannerView?.setBannerFormat(it)
                }
            )
            Spacer(modifier = Modifier.padding(top = 2.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppButton(text = "Create") {
                    bannerExists.value = true
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(
                    text = "Load",
                ) {
                    bannerView?.loadAd(activity = activity)
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
                    logInfo(TAG, "Recompose. ShowClicked: $bannerView")
                    bannerView?.showAd()
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppButton(text = "Destroy") {
                    bannerView?.destroyAd()
                    bannerView = null
                    bannerExists.value = false
                    logFlow.log("BannerView destroyed")
                }
            }
            Row {
                AppTextButton(text = "Notify Loss") {
                    bannerView?.also {
                        it.notifyLoss(winnerDemandId = "Unity", winnerPrice = 4.0)
                        logFlow.log("NotifyLoss")
                    }
                }
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                AppTextButton(text = "Notify Win") {
                    bannerView?.also {
                        it.notifyWin()
                        logFlow.log("NotifyWin")
                    }
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
    logInfo(TAG, string)
}

private const val TAG = "BannerScreen"
private const val DefaultAutoRefreshTimeoutMs = 10000L