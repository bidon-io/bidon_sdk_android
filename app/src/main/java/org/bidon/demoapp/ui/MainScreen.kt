package org.bidon.demoapp.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.bidon.bidmachine.BidMachineAdapter
import org.bidon.demoapp.BuildConfig
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.CaptionText
import org.bidon.demoapp.component.H5Text
import org.bidon.demoapp.navigation.Screen
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.utils.networking.NetworkSettings

@Composable
internal fun MainScreen(
    navController: NavHostController,
    initState: MutableState<MainScreenState>,
    sharedPreferences: SharedPreferences
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
        when (val state = initState.value) {
            MainScreenState.NotInitialized,
            MainScreenState.Initializing -> {
                H5Text(text = "Bidon")
                CaptionText(
                    text = BuildConfig.APPLICATION_ID,
                    modifier = Modifier.padding(top = 4.dp, bottom = 40.dp),
                    color = Color.White.copy(alpha = 0.4f)
                )
                if (state == MainScreenState.NotInitialized) {
                    AppButton(text = "Init") {
                        val baseUrl =
                            sharedPreferences.getString("host", NetworkSettings.BidonBaseUrl) ?: NetworkSettings.BidonBaseUrl
                        initState.value = MainScreenState.Initializing
                        BidonSdk
                            .setLoggerLevel(Logger.Level.Verbose)
                            .registerDefaultAdapters()
                            .registerAdapters(BidMachineAdapter())
                            .registerAdapter("org.bidon.admob.AdmobAdapter")
                            .setBaseUrl(baseUrl)
                            .setInitializationCallback {
                                initState.value = MainScreenState.Initialized
                            }
                            .initialize(
                                activity = context as Activity,
                                appKey = BuildConfig.BIDON_API_KEY,
                            )
                    }
                } else {
                    CircularProgressIndicator()
                }
                AppTextButton(text = "Server settings", modifier = Modifier.padding(top = 30.dp)) {
                    navController.navigate(Screen.ServerSettings.route)
                }
            }
            MainScreenState.Initialized -> {
                H5Text(text = "Ad types")
                CaptionText(
                    text = BuildConfig.APPLICATION_ID,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
                    color = Color.White.copy(alpha = 0.4f)
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
//                AppButton(text = "Banner in XML-Layout") {
//                    (context as Activity).startActivity(
//                        Intent(context, BannerViewActivity::class.java)
//                    )
//                }
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