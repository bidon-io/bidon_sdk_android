package org.bidon.demoapp.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.AppToolbar
import org.bidon.demoapp.component.Body2Text
import org.bidon.demoapp.component.ItemSelector
import org.bidon.demoapp.ui.domain.BannerManagerViewModel
import org.bidon.demoapp.ui.ext.toUiString
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.ads.banner.BannerPosition
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import java.lang.Math.random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PositionedBannerScreen(
    navController: NavHostController,
    viewModel: BannerManagerViewModel
) {
    val logFlow = remember {
        mutableStateOf(listOf<String>())
    }
    val configuration = LocalConfiguration.current
    val bannerFormat = remember {
        mutableStateOf(BannerFormat.Adaptive)
    }
    val banner = viewModel.bannerManager.apply {
        setBannerFormat(bannerFormat.value)
        setBannerListener(
            object : BannerListener {
                override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                    logFlow.log("onAdLoaded ad: ${ad.toUiString()}. auctionInfo: ${auctionInfo.toUiString()}")
                }

                override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                    logFlow.log("onAdLoadFailed: $cause. auctionInfo: ${auctionInfo?.toUiString()}")
                }

                override fun onAdShown(ad: Ad) {
                    logFlow.log("onAdShown: ${ad.toUiString()}")
                }

                override fun onAdClicked(ad: Ad) {
                    logFlow.log("onAdClicked: ${ad.toUiString()}")
                }

                override fun onAdExpired(ad: Ad) {
                    logFlow.log("onAdExpired: ${ad.toUiString()}")
                }

                override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                    logFlow.log("onRevenuePaid: ad=${ad.toUiString()}, adValue=$adValue")
                }

                override fun onAdShowFailed(cause: BidonError) {
                    logFlow.log("onAdShowFailed: $cause")
                }
            }
        )
    }
    val activity = LocalContext.current as Activity
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val bannerPosition = remember {
        mutableStateOf<BannerPosition?>(BannerPosition.HorizontalTop)
    }

    logInfo("PositionedBannerScreen", "banner: $banner")

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(52.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            AppToolbar(
                title = "Positioned Banners",
                onNavigationButtonClicked = { navController.popBackStack() }
            )
            Column(modifier = Modifier.padding(8.dp)) {
                ItemSelector(
                    title = "Format",
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
                        banner.setBannerFormat(it)
                    }
                )
                Spacer(modifier = Modifier.padding(top = 10.dp))
                ItemSelector(
                    title = "Position",
                    items = BannerPosition.values().toList(),
                    selectedItem = bannerPosition.value,
                    horizontalAlignment = Alignment.Start,
                    getItemTitle = {
                        when (it) {
                            BannerPosition.HorizontalTop -> "Top"
                            BannerPosition.HorizontalBottom -> "Bottom"
                            BannerPosition.VerticalLeft -> "Left"
                            BannerPosition.VerticalRight -> "Right"
                        }
                    },
                    onItemClicked = {
                        bannerPosition.value = it
                        banner.setPosition(it)
                    }
                )
                Spacer(modifier = Modifier.padding(top = 10.dp))
                AppTextButton(text = "Custom Position") {
                    bannerPosition.value = null
                    banner.setCustomPosition(
                        offset = android.graphics.Point(
                            (random() * configuration.screenWidthDp).toInt(),
                            (random() * configuration.screenHeightDp).toInt(),
                        ),
                        rotation = (random() * 360).toInt(),
                        anchor = android.graphics.PointF(random().toFloat(), random().toFloat())
                    )
                }
                Spacer(modifier = Modifier.padding(top = 10.dp))
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    AppButton(
                        modifier = Modifier.padding(end = 12.dp),
                        text = "Load",
                    ) {
                        banner.loadAd(activity, pricefloor = 0.001)
                    }
                    AppButton(
                        modifier = Modifier.padding(end = 12.dp),
                        text = "Show",
                    ) {
                        banner.setBannerFormat(bannerFormat.value)
                        banner.setPosition(bannerPosition.value ?: return@AppButton)
                        banner.showAd(activity)
                    }
                    AppButton(
                        modifier = Modifier.padding(end = 12.dp),
                        text = "Hide",
                    ) {
                        banner.hideAd(activity)
                    }
                    AppButton(
                        modifier = Modifier.padding(end = 12.dp),
                        text = "Destroy",
                    ) {
                        banner.destroyAd(activity)
                    }
                }
            }
        }
        if (logFlow.value.isNotEmpty()) {
            item {
                Text(
                    text = "Clear Log",
                    modifier = Modifier
                        .padding(2.dp)
                        .clickable {
                            logFlow.value = listOf()
                        },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
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

private fun MutableState<List<String>>.log(string: String) {
    synchronized(this) {
        this.value = this.value + string
    }
    logInfo(TAG, string)
}

private const val TAG = "PositionedBannerScreen"
