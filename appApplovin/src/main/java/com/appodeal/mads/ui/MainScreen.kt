package com.appodeal.mads.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.Body2Text
import com.appodeal.mads.component.H5Text
import com.appodeal.mads.component.Subtitle1Text
import com.appodeal.mads.navigation.Screen

@Composable
fun MainScreen(
    navController: NavHostController,
    initState: MutableState<Boolean>,
) {
    val viewModel = remember {
        MainViewModel()
    }
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
                modifier = Modifier.padding(top = 40.dp, bottom = 8.dp),
                text = "PostBid Demands"
            )
            Box(
                modifier = Modifier.fillMaxWidth(0.35f)
            ) {
                Body2Text(text = "BidMachine", modifier = Modifier.align(Alignment.CenterStart))
                Icon(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    imageVector = Icons.Default.Check,
                    contentDescription = "",
                    tint = MaterialTheme.colors.primary
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(0.35f)
            ) {
                Body2Text(text = "Admob", modifier = Modifier.align(Alignment.CenterStart))
                Icon(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    imageVector = Icons.Default.Check,
                    contentDescription = "",
                    tint = MaterialTheme.colors.primary
                )
            }

            Subtitle1Text(
                modifier = Modifier.padding(top = 40.dp, bottom = 8.dp),
                text = "Applovin API Decorator"
            )
            AppButton(text = "Init") {
                viewModel.initSdk(
                    activity = context as Activity,
                    onInitialized = {
                        initState.value = true
                    }
                )
            }
        } else {

            H5Text(
                modifier = Modifier.padding(bottom = 24.dp),
                text = "Applovin API"
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