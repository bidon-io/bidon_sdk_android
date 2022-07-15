package com.appodeal.mads.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.appodeal.mads.component.AppToolbar
import com.appodeal.mads.setBannerListener
import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.mads.demands.banners.BannerSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun BannerApplovinScreen(navController: NavHostController, viewModel: BannerApplovinViewModel) {
    val context = LocalContext.current

    val state = viewModel.stateFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Banner (Applovin API)",
            onNavigationButtonClicked = { navController.popBackStack() }
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .dashedBorder(width = 1.dp, radius = 4.dp, color = MaterialTheme.colors.error)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bannerView = state.value.bannerAdView
            if (bannerView != null) {
                AndroidView(
                    modifier = Modifier.height(
                        when (state.value.adFormat) {
                            BannerSize.Banner -> 50.dp
                            BannerSize.LeaderBoard -> 90.dp
                            BannerSize.MRec -> 250.dp
                            else -> error("Not supported")
                        }
                    ),
                    factory = {
                        bannerView
                    })
            } else {
                Subtitle1Text(text = "Place for Banner")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            ItemSelector(
                title = "Ad Format",
                items = listOf(BannerSize.Banner, BannerSize.LeaderBoard, BannerSize.MRec),
                selectedItem = state.value.adFormat,
                getItemTitle = {
                    when (it) {
                        BannerSize.Banner -> "Banner 320x50"
                        BannerSize.LeaderBoard -> "Leader Board 728x90"
                        BannerSize.MRec -> "MRec 300x250"
                        else -> error("Not supported")
                    }
                },
                onItemClicked = {
                    viewModel.setBannerSize(bannerSize = it)
                }
            )
            Spacer(modifier = Modifier.padding(top = 16.dp))
            AppButton(text = "Create banner") {
                viewModel.createAd(context)
            }
            if (state.value.bannerAdView != null) {
                AppButton(text = "Load") {
                    viewModel.loadAd()
                }
                AppButton(text = "Destroy") {
                    viewModel.destroyAd()
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
            ) {
                items(state.value.logs) { logLine ->
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

class BannerApplovinViewModel {
    class State(
        val logs: List<String>,
        val bannerAdView: BNMaxAdView?,
        val adFormat: BannerSize
    )

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    val stateFlow = MutableStateFlow(
        State(
            logs = listOf("Logs"),
            bannerAdView = null,
            adFormat = BannerSize.Banner
        )
    )

    fun createAd(context: Context) {
        val state = stateFlow.value
        destroyAd()
        updateState(
            State(
                bannerAdView = BNMaxAdView(
                    adUnitId = "c7c5f664e60b9bfb",
                    adFormat = state.adFormat,
                    context = context
                ).also {
                    it.setBannerListener { log ->
                        synchronized(this) {
                            val state1 = stateFlow.value
                            updateState(
                                State(
                                    adFormat = state1.adFormat,
                                    logs = state1.logs + log,
                                    bannerAdView = state1.bannerAdView
                                )
                            )
                        }
                    }
                },
                logs = state.logs,
                adFormat = state.adFormat
            )
        )
    }

    fun loadAd() {
        stateFlow.value.bannerAdView?.loadAd()
    }

    fun destroyAd() {
        stateFlow.value.bannerAdView?.destroy()
        val state = stateFlow.value
        updateState(
            State(
                adFormat = state.adFormat,
                logs = state.logs,
                bannerAdView = null
            )
        )
    }

    fun setBannerSize(bannerSize: BannerSize) {
        val state = stateFlow.value
        updateState(
            State(
                adFormat = bannerSize,
                logs = state.logs,
                bannerAdView = state.bannerAdView
            )
        )
    }

    private fun updateState(newState: State) {
        coroutineScope.launch {
            stateFlow.emit(newState)
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