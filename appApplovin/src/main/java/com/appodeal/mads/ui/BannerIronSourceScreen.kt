package com.appodeal.mads.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.appodeal.mads.component.*
import com.appodeal.mads.ui.listener.createIronSourceBannerListener
import com.appodealstack.ironsource.IronSourceDecorator
import com.appodealstack.ironsource.banner.BNIronSourceBannerLayout
import com.appodealstack.mads.core.DefaultAutoRefreshTimeoutMs
import com.appodealstack.mads.demands.banners.BannerSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun BannerIronSourceScreen(navController: NavHostController, viewModel: BannerIronSourceViewModel) {
    val context = LocalContext.current
    val getHeightByWindowsScreen: Int = 90// TODO()
    val state = viewModel.stateFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Banner (IronSource API)",
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
                            BannerSize.Large -> 90.dp
                            BannerSize.MRec -> 250.dp
                            BannerSize.Smart -> getHeightByWindowsScreen.dp
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
                items = listOf(BannerSize.Banner, BannerSize.Large, BannerSize.MRec),// TODO implement - BannerSize.Smart
                selectedItem = state.value.adFormat,
                getItemTitle = {
                    when (it) {
                        BannerSize.Banner -> "Banner 320x50"
                        BannerSize.MRec -> "Rectangle 300x250"
                        BannerSize.Large -> "Large 320x90"
                        BannerSize.Smart -> "Smart 320x50/728x90"
                        else -> error("Not supported")
                    }
                },
                onItemClicked = {
                    viewModel.setBannerSize(bannerSize = it)
                }
            )
            Spacer(modifier = Modifier.padding(top = 16.dp))
            val autoRefreshText = "AutoRefresh " + if (state.value.autoRefreshTtl / 1000 == 0L) {
                "Off"
            } else {
                "each ${state.value.autoRefreshTtl / 1000} sec."
            }
            Body2Text(text = autoRefreshText)
            Slider(
                value = (state.value.autoRefreshTtl / 1000).toFloat(),
                onValueChange = {
                    viewModel.setAutoRefresh((it * 1000).toLong())
                },
                steps = 30,
                valueRange = 0f..30f
            )
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

class BannerIronSourceViewModel {
    class State(
        val logs: List<String>,
        val bannerAdView: BNIronSourceBannerLayout?,
        val adFormat: BannerSize,
        val autoRefreshTtl: Long,
    )

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    val stateFlow = MutableStateFlow(
        State(
            logs = listOf("Logs"),
            bannerAdView = null,
            adFormat = BannerSize.Banner,
            autoRefreshTtl = DefaultAutoRefreshTimeoutMs
        )
    )

    fun createAd(context: Context) {
        destroyAd()
        val banner = IronSourceDecorator.createBanner(context as Activity, stateFlow.value.adFormat)
            .also {
                it.setLevelPlayBannerListener(createIronSourceBannerListener { log ->
                    synchronized(this) {
                        val state = stateFlow.value
                        updateState(
                            State(
                                bannerAdView = state.bannerAdView,
                                logs = state.logs + log,
                                adFormat = state.adFormat,
                                autoRefreshTtl = state.autoRefreshTtl,
                            )
                        )
                    }
                })
            }
        val state = stateFlow.value
        updateState(
            State(
                bannerAdView = banner,
                logs = state.logs,
                adFormat = state.adFormat,
                autoRefreshTtl = state.autoRefreshTtl,
            )
        )
    }

    fun loadAd() {
        stateFlow.value.bannerAdView?.let {
            IronSourceDecorator.loadBanner(it)
        }
    }

    fun destroyAd() {
        stateFlow.value.bannerAdView?.let {
            IronSourceDecorator.destroyBanner(it)
        }
        val state = stateFlow.value
        updateState(
            State(
                adFormat = state.adFormat,
                logs = state.logs,
                bannerAdView = null,
                autoRefreshTtl = state.autoRefreshTtl,
            )
        )
    }

    fun setBannerSize(bannerSize: BannerSize) {
        val state = stateFlow.value
        updateState(
            State(
                adFormat = bannerSize,
                logs = state.logs,
                bannerAdView = state.bannerAdView,
                autoRefreshTtl = state.autoRefreshTtl,
            )
        )
    }

    private fun updateState(newState: State) {
        coroutineScope.launch {
            stateFlow.emit(newState)
        }
    }

    fun setAutoRefresh(ttlMs: Long) {
        val state = stateFlow.value
        stateFlow.value.bannerAdView?.let {
            if (ttlMs == 0L) {
                IronSourceDecorator.stopAutoRefresh(it)
            } else {
                IronSourceDecorator.setAutoRefreshTimeout(it, ttlMs)
            }
        }
        updateState(
            State(
                adFormat = state.adFormat,
                logs = state.logs,
                bannerAdView = state.bannerAdView,
                autoRefreshTtl = ttlMs,
            )
        )
    }
}