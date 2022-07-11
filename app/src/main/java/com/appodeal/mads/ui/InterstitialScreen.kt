package com.appodeal.mads.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.Body1Text
import com.appodeal.mads.component.H5Text
import com.appodeal.mads.setInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
fun InterstitialScreen(
    viewModel: InterstitialViewModel = InterstitialViewModel()
) {
    val activity = LocalContext.current as Activity

    LaunchedEffect(key1 = Unit, block = {
        viewModel.createAd(activity)
    })

    val logState = viewModel.logFlow.asStateFlow()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        H5Text(
            text = "Interstitial",
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
        )
        AppButton(text = "Load") {
            viewModel.loadAd()
        }
        AppButton(text = "Show") {
            viewModel.showAd()
        }
        Body1Text(text = logState.value)
    }
}

class InterstitialViewModel {
    private lateinit var interstitialAd: BNMaxInterstitialAd

    val logFlow = MutableStateFlow("")

    fun createAd(activity: Activity) {
        interstitialAd = BNMaxInterstitialAd("c7c5f664e60b9bfb", activity)
        setInterstitialListener()
    }

    private fun setInterstitialListener() {
        interstitialAd.setInterstitialListener { log ->
            logFlow.value = logFlow.value + "\n\n" + log
        }
    }

    fun loadAd() {
        interstitialAd.loadAd()
    }

    fun showAd() {
        interstitialAd.showAd()
    }
}