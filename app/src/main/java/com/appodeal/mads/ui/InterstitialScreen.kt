package com.appodeal.mads.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.*
import com.appodeal.mads.component.AppToolbar
import com.appodeal.mads.setInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.mads.core.ext.logInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Composable
fun InterstitialScreen(navController: NavHostController, viewModel: InterstitialViewModel) {
    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = Unit, block = {
        viewModel.createAd(activity)
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

    val logFlow = MutableStateFlow(listOf("Log"))

    fun createAd(activity: Activity) {
        interstitialAd = BNMaxInterstitialAd("c7c5f664e60b9bfb", activity)
        setInterstitialListener()
    }

    private fun setInterstitialListener() {
        interstitialAd.setInterstitialListener(
            log = { log ->
                coroutineScope.launch {
                    logFlow.emit(logFlow.value + log)
                }
            })
    }

    fun loadAd() {
        interstitialAd.loadAd()
    }

    fun showAd() {
        interstitialAd.showAd()
    }
}