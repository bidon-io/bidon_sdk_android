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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.bidon.demoapp.BuildConfig
import org.bidon.demoapp.component.*
import org.bidon.demoapp.navigation.Screen
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.utils.networking.NetworkSettings
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
internal fun MainScreen(
    navController: NavHostController,
    initState: MutableState<MainScreenState>,
    sharedPreferences: SharedPreferences
) {
    val adapters = remember {
        mutableStateOf(DefaultAdapters.values().toList())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    MultiSelector(
                        modifier = Modifier.padding(horizontal = 60.dp, vertical = 16.dp),
                        items = DefaultAdapters.values().toList(),
                        selectedItems = adapters.value,
                        getItemTitle = {
                            when (it) {
                                DefaultAdapters.AdmobAdapter -> "Admob"
                                DefaultAdapters.BidmachineAdapter -> "BidMachine"
                                DefaultAdapters.ApplovinAdapter -> "Applovin"
                                DefaultAdapters.DataExchangeAdapter -> "DT Exchange"
                                DefaultAdapters.UnityAdsAdapter -> "Unity Ads"
                            }
                        },
                        onItemClicked = {
                            adapters.value = if (it in adapters.value) {
                                adapters.value - it
                            } else {
                                adapters.value + it
                            }
                        }
                    )
                    AppOutlinedButton(
                        modifier = Modifier.padding(top = 16.dp),
                        text = "Add SDK-level Extras"
                    ) {
                        BidonSdk.addExtra("sdk_level_string_before_init", "string0")
                        BidonSdk.addExtra("sdk_level_int_before_init", 555)
                    }
                    AppButton(text = "Init") {
                        val baseUrl =
                            sharedPreferences.getString("host", NetworkSettings.BidonBaseUrl) ?: NetworkSettings.BidonBaseUrl
                        initState.value = MainScreenState.Initializing
                        BidonSdk
                            .setLoggerLevel(Logger.Level.Verbose)
                            .apply {
                                adapters.value.forEach {
                                    registerAdapter(it.classPath)
                                }
                            }
//                            .registerDefaultAdapters()
//                            .registerAdapters(ApplovinAdapter())
//                            .registerAdapter("org.bidon.admob.AdmobAdapter")
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
                AppOutlinedButton(
                    modifier = Modifier.padding(top = 16.dp),
                    text = "Add SDK-level Extras"
                ) {
                    BidonSdk.addExtra("sdk_level_long_after_init", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
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