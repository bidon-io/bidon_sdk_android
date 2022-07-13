package com.appodeal.mads.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.*
import com.appodeal.mads.component.AppToolbar
import com.appodeal.mads.setInterstitialListener
import com.appodeal.mads.ui.listener.createFyberInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.fyber.interstitial.BNFyberInterstitial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun InterstitialScreen(
    navController: NavHostController,
    viewModel: InterstitialViewModel,
    sdk: MediationSdk
) {
    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = Unit, block = {
        viewModel.createAd(activity, sdk)
    })

    val logState = viewModel.logFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Interstitial",
            onNavigationButtonClicked = { navController.popBackStack() }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            AppButton(text = "Load") {
                viewModel.loadAd()
            }
            AppButton(text = "Show") {
                viewModel.showAd()
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
            ) {
                items(logState.value) { logLine ->
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

class InterstitialViewModel {
    private lateinit var interstitialAd: BNMaxInterstitialAd
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private lateinit var sdk: MediationSdk
    private var placementId = "197405"
    private var activity: Activity? = null

    val logFlow = MutableStateFlow(listOf("Log"))

    fun createAd(activity: Activity, sdk: MediationSdk) {
        this.activity = activity
        this.sdk = sdk
        when (sdk) {
            MediationSdk.None -> Unit
            MediationSdk.Applovin -> {
                interstitialAd = BNMaxInterstitialAd("c7c5f664e60b9bfb", activity)
                interstitialAd.setInterstitialListener(
                    log = { log ->
                        coroutineScope.launch {
                            logFlow.emit(logFlow.value + log)
                        }
                    })
            }
            MediationSdk.Fyber -> {
                BNFyberInterstitial.setInterstitialListener(
                    createFyberInterstitialListener { log ->
                        coroutineScope.launch {
                            logFlow.emit(logFlow.value + log)
                        }
                    }
                )
            }
        }
    }

    fun loadAd() {
        when (sdk) {
            MediationSdk.None -> Unit
            MediationSdk.Applovin -> {
                interstitialAd.loadAd()
            }
            MediationSdk.Fyber -> {
                BNFyberInterstitial.request(placementId, activity)
            }
        }
    }

    fun showAd() {
        when (sdk) {
            MediationSdk.None -> Unit
            MediationSdk.Applovin -> {
                interstitialAd.showAd()
            }
            MediationSdk.Fyber -> {
                activity?.let {
                    BNFyberInterstitial.show(placementId, it)
                }
            }
        }
    }
}