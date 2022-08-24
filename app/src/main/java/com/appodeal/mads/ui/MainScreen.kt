package com.appodeal.mads.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.appodeal.mads.component.AppButton
import com.appodeal.mads.component.AppTextButton
import com.appodeal.mads.component.H5Text
import com.appodeal.mads.navigation.Screen
import com.appodealstack.bidon.BidON
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorageImpl

@Composable
internal fun MainScreen(
    navController: NavHostController,
    initState: MutableState<MainScreenState>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val context = LocalContext.current
        val keyValueStorage: KeyValueStorage = KeyValueStorageImpl().apply {
            init(context)
        }
        when (val state = initState.value) {
            MainScreenState.NotInitialized,
            MainScreenState.Initializing -> {
                H5Text(
                    modifier = Modifier.padding(bottom = 40.dp),
                    text = "BidON"
                )
                if (state == MainScreenState.NotInitialized) {
                    AppButton(text = "Init") {
                        initState.value = MainScreenState.Initializing
                        BidON
                            .setBaseUrl(keyValueStorage.host)
                            .init(
                                activity = context as Activity,
                                appKey = "YOUR_APP_KEY",
                            ) {
                                initState.value = MainScreenState.Initialized
                            }
                    }
                } else {
                    CircularProgressIndicator()
                }
                AppTextButton(text = "Server settings", modifier = Modifier.padding(top = 30.dp)) {
                    navController.navigate(Screen.ServerSettings.route)
                }
            }
            MainScreenState.Initialized -> {
                H5Text(
                    modifier = Modifier.padding(bottom = 24.dp),
                    text = "Ad types"
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
}

internal enum class MainScreenState {
    NotInitialized,
    Initializing,
    Initialized,
}