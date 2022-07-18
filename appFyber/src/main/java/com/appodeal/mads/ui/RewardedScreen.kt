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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.AppToolbar
import com.appodeal.mads.component.Body2Text
import com.appodeal.mads.ui.listener.createFyberRewardedListener
import com.appodealstack.fyber.rewarded.BNFyberRewarded

@Composable
fun RewardedScreen(
    navController: NavHostController,
) {
    val rewardedPlacementId = "197406"
    val activity = LocalContext.current as Activity
    val logFlow = remember {
        mutableStateOf(listOf("Log"))
    }

    LaunchedEffect(key1 = Unit, block = {
        BNFyberRewarded.setRewardedListener(
            createFyberRewardedListener { log ->
                logFlow.value = logFlow.value + log
            })
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Fyber Rewarded",
            onNavigationButtonClicked = { navController.popBackStack() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            AppButton(text = "Load") {
                BNFyberRewarded.request(rewardedPlacementId, activity)
            }
            AppButton(text = "Show") {
                BNFyberRewarded.show(rewardedPlacementId, activity)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 24.dp)
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
            }
        }
    }
}