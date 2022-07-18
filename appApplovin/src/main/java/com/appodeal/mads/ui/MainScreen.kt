package com.appodeal.mads.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.*
import com.appodeal.mads.navigation.Screen
import com.appodealstack.admob.AdmobAdapter
import com.appodealstack.admob.AdmobParameters
import com.appodealstack.applovin.AppLovinDecorator
import com.appodealstack.applovin.ApplovinMaxAdapter
import com.appodealstack.applovin.ApplovinParameters
import com.appodealstack.bidmachine.BidMachineAdapter
import com.appodealstack.bidmachine.BidMachineParameters
import com.appodealstack.fyber.FairBidDecorator
import com.appodealstack.ironsource.IronSourceDecorator

enum class MediationSdk {
    None,
    Applovin,
    Fyber,
    IronSource,
}

@Composable
fun MainScreen(
    navController: NavHostController,
    initState: MutableState<Boolean>,
    initMediation: MutableState<MediationSdk>
) {
    val viewModel = remember {
        MainViewModel()
    }
    val state = viewModel.stateFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current
        if (!initState.value) {
            H5Text(
                modifier = Modifier.padding(bottom = 40.dp),
                text = "BidOn Initialization"
            )
            Subtitle1Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = "Mediation Demands"
            )
            Box(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Body2Text(text = "Applovin", modifier = Modifier.align(Alignment.CenterStart))
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    checked = state.value.applovin,
                    onCheckedChange = {
                        viewModel.setChecked(applovin = it)
                    }
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Body2Text(text = "Fyber FairBid", modifier = Modifier.align(Alignment.CenterStart))
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    checked = state.value.fyber,
                    onCheckedChange = {
                        viewModel.setChecked(fyber = it)
                    }
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Body2Text(text = "ironSource", modifier = Modifier.align(Alignment.CenterStart))
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    checked = state.value.ironSource,
                    onCheckedChange = {
                        viewModel.setChecked(ironSource = it)
                    }
                )
            }

            Subtitle1Text(
                modifier = Modifier.padding(top = 40.dp, bottom = 8.dp),
                text = "PostBid Demands"
            )
            Box(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Body2Text(text = "BidMachine", modifier = Modifier.align(Alignment.CenterStart))
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    checked = true,
                    onCheckedChange = {}
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                Body2Text(text = "Admob", modifier = Modifier.align(Alignment.CenterStart))
                Checkbox(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    checked = true,
                    onCheckedChange = {}
                )
            }

            Subtitle1Text(
                modifier = Modifier.padding(top = 40.dp, bottom = 8.dp),
                text = "Select API Decorator"
            )
            AppButton(text = "Init with Applovin API") {
                viewModel.initSdk(
                    activity = context as Activity,
                    sdkApi = MediationSdk.Applovin,
                    onInitialized = {
                        initState.value = true
                        initMediation.value = MediationSdk.Applovin
                    }
                )
            }
            CaptionText(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "Applovin" + (", ironSource".takeIf { state.value.ironSource } ?: "")
                        + (", FairBid".takeIf { state.value.fyber } ?: "")
                        + " will be initialized"
            )

            AppButton(text = "Init with Fyber API") {
                viewModel.initSdk(
                    activity = context as Activity,
                    sdkApi = MediationSdk.Fyber,
                    onInitialized = {
                        initState.value = true
                        initMediation.value = MediationSdk.Fyber
                    }
                )
            }
            CaptionText(
                modifier = Modifier.padding(bottom = 16.dp),
                text = "Fyber" + (", ironSource".takeIf { state.value.ironSource } ?: "")
                        + (", Applovin".takeIf { state.value.applovin } ?: "")
                        + " will be initialized"
            )

            AppButton(text = "Init with IronSource API") {
                viewModel.initSdk(
                    activity = context as Activity,
                    sdkApi = MediationSdk.IronSource,
                    onInitialized = {
                        initState.value = true
                        initMediation.value = MediationSdk.IronSource
                    }
                )
            }
            CaptionText(
                modifier = Modifier.padding(bottom = 12.dp),
                text = "ironSource" + (", FairBid".takeIf { state.value.fyber } ?: "")
                        + (", Applovin".takeIf { state.value.applovin } ?: "")
                        + " will be initialized"
            )
        } else {

            H5Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text = when (initMediation.value) {
                    MediationSdk.None -> ""
                    MediationSdk.Applovin -> "Applovin MAX API"
                    MediationSdk.Fyber -> "Fyber FairBid API"
                    MediationSdk.IronSource -> "IronSource API"
                }
            )
            AppButton(text = "Interstitial") {
                navController.navigate(Screen.Interstitial.route)
            }
            AppButton(text = "Rewarded") {
                navController.navigate(Screen.Rewarded.route)
            }
            AppButton(text = "Banner") {
                navController.navigate(Screen.Banners.route)
            }
            TextButton(modifier = Modifier.padding(top = 100.dp), onClick = {
                val packageManager: PackageManager = context.packageManager
                val intent: Intent = packageManager.getLaunchIntentForPackage(context.packageName)!!
                val componentName: ComponentName = intent.component!!
                val restartIntent: Intent = Intent.makeRestartActivityTask(componentName)
                context.startActivity(restartIntent)
                Runtime.getRuntime().exit(0)
            }) {
                Text(text = "Restart / Re-Init application")
            }
        }
    }
}