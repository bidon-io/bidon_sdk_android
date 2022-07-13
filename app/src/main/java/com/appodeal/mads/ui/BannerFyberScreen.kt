package com.appodeal.mads.ui

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.AppToolbar
import com.appodeal.mads.component.Body2Text
import com.appodeal.mads.component.Subtitle1Text
import com.appodeal.mads.ui.listener.createFyberBannerListener
import com.appodealstack.fyber.banner.BNFyberBanner
import com.appodealstack.fyber.banner.BNFyberBannerOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun BannerFyberScreen(navController: NavHostController, viewModel: BannerFyberViewModel) {
    val placementId = "197407"
    val context = LocalContext.current
    val adContainer = FrameLayout(context).apply {
        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    val state = viewModel.stateFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
    ) {
        AppToolbar(
            title = "Banner",
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
            val viewGroup = state.value.adContainer
            if (viewGroup != null) {
                AndroidView(factory = {
                    viewGroup
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
            AppButton(text = "Load & show") {
                viewModel.show(placementId, BNFyberBannerOption().placeInContainer(adContainer), context as Activity)
            }
            if (state.value.adContainer != null) {
                AppButton(text = "Destroy") {
                    viewModel.destroyAd(placementId)
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

class BannerFyberViewModel {
    class State(
        val logs: List<String>,
        val adContainer: ViewGroup?
    )

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    val stateFlow = MutableStateFlow(
        State(
            logs = listOf("Logs"),
            adContainer = null
        )
    )

    init {
        BNFyberBanner.setBannerListener(createFyberBannerListener {
            logMe(it)
        })
    }

    fun show(placementId: String, placeInContainer: BNFyberBannerOption, activity: Activity) {
        BNFyberBanner.show(placementId, placeInContainer, activity)
        updateState(
            State(
                logs = stateFlow.value.logs,
                adContainer = (placeInContainer.getPosition() as? BNFyberBannerOption.Position.InViewGroup)?.viewGroup
            )
        )
    }

    fun destroyAd(placementId: String) {
        BNFyberBanner.destroy(placementId)
    }

    private fun logMe(log: String) {
        val state = stateFlow.value
        updateState(
            State(
                logs = state.logs + log,
                adContainer = state.adContainer
            )
        )
    }

    private fun updateState(newState: State) {
        coroutineScope.launch {
            stateFlow.emit(newState)
        }
    }
}