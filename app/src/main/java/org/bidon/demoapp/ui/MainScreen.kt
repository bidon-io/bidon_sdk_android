package org.bidon.demoapp.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import org.bidon.demoapp.BannerViewActivity
import org.bidon.demoapp.BuildConfig
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.Body1Text
import org.bidon.demoapp.component.CaptionText
import org.bidon.demoapp.component.H5Text
import org.bidon.demoapp.component.ItemSelector
import org.bidon.demoapp.component.MultiSelector
import org.bidon.demoapp.navigation.Screen
import org.bidon.demoapp.theme.AppColors
import org.bidon.demoapp.ui.FullscreenModeExt.immersiveSystemUI
import org.bidon.demoapp.ui.FullscreenModeExt.translucentSystemUI
import org.bidon.demoapp.ui.settings.AppBaqendBaseUrl
import org.bidon.demoapp.ui.settings.TestModeInfo
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr

@Composable
internal fun MainScreen(
    navController: NavHostController,
    initState: MutableState<MainScreenState>,
    sharedPreferences: SharedPreferences
) {
    val activity = LocalContext.current as Activity
    val shared = LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE)

    val adapters = remember {
        mutableStateOf(DefaultAdapters.values().sortedBy { it.name })
    }
    val isTestMode = TestModeInfo.isTesMode.collectAsState()
    val fullscreenModeState = remember {
        mutableStateOf(
            shared.getInt("fullscreen", FullscreenMode.Default.code).let { code ->
                FullscreenMode.values().first { it.code == code }
            }.also {
                setFullscreenMode(it, activity)
            }
        )
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
                    modifier = Modifier.padding(top = 4.dp, bottom = 0.dp),
                    color = Color.White.copy(alpha = 0.4f)
                )
                TestModeView(isTestMode)
                if (state == MainScreenState.NotInitialized) {
                    MultiSelector(
                        modifier = Modifier.padding(start = 60.dp, end = 60.dp, top = 16.dp),
                        items = DefaultAdapters.values().sortedBy { it.name },
                        selectedItems = adapters.value,
                        getItemTitle = {
                            it.name.substringBefore("Adapter")
                        },
                        onItemClicked = {
                            adapters.value = if (it in adapters.value) {
                                adapters.value - it
                            } else {
                                adapters.value + it
                            }
                        }
                    )
                    AppTextButton(text = "Deselect All", modifier = Modifier.padding(bottom = 16.dp)) {
                        adapters.value = emptyList()
                    }
                    ItemSelector(
                        modifier = Modifier.padding(top = 16.dp),
                        title = "Fullscreen Mode",
                        items = FullscreenMode.values().toList(),
                        selectedItem = fullscreenModeState.value,
                        getItemTitle = {
                            when (it) {
                                // FullscreenMode.Normal -> "Normal"
                                FullscreenMode.TranslucentNavigation -> "Translucent"
                                FullscreenMode.Immersive -> "Immersive"
                            }
                        },
                        onItemClicked = {
                            fullscreenModeState.value = it
                            shared.edit {
                                putInt("fullscreen", it.code)
                            }
                            setFullscreenMode(it, activity)
                        }
                    )
                    AppTextButton(text = "Server settings", modifier = Modifier.padding(top = 0.dp)) {
                        navController.navigate(Screen.ServerSettings.route)
                    }
                    AppButton(text = "Init") {
                        val baseUrl =
                            sharedPreferences.getString("host", AppBaqendBaseUrl) ?: AppBaqendBaseUrl
                        BidonSdk.setTestMode(isTestMode.value)
                        BidonSdk.regulation.gdpr = sharedPreferences.getInt("gdpr", Gdpr.Default.code).let { code ->
                            Gdpr.values().first { it.code == code }.also { gdpr ->
                                BidonSdk.regulation.gdprConsentString = "1YYY".takeIf { gdpr == Gdpr.Given }
                            }
                        }
                        BidonSdk.regulation.coppa = sharedPreferences.getInt("coppa", Coppa.Default.code).let { code ->
                            Coppa.values().first { it.code == code }.also { coppa ->
                                BidonSdk.regulation.usPrivacyString = "1YYY".takeIf { coppa == Coppa.Yes }
                            }
                        }

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
                                context = context,
                                appKey = BuildConfig.BIDON_API_KEY,
                            )
                    }
                } else {
                    CircularProgressIndicator()
                }
            }

            MainScreenState.Initialized -> {
                H5Text(text = "Ad types")
                CaptionText(
                    text = BuildConfig.APPLICATION_ID,
                    modifier = Modifier.padding(top = 4.dp, bottom = 0.dp),
                    color = Color.White.copy(alpha = 0.4f)
                )
                TestModeView(isTestMode)

                Spacer(modifier = Modifier.padding(bottom = 24.dp))
                AppButton(text = "Interstitial") {
                    navController.navigate(Screen.Interstitial.route)
                }
                AppButton(text = "Rewarded") {
                    navController.navigate(Screen.Rewarded.route)
                }
                AppButton(text = "Banner") {
                    navController.navigate(Screen.Banners.route)
                }
                AppButton(text = "Banner in XML-Layout") {
                    (context as Activity).startActivity(
                        Intent(context, BannerViewActivity::class.java)
                    )
                }
                AppButton(text = "Positioned Banner") {
                    navController.navigate(Screen.PositionedBanners.route)
                }
                TextButton(modifier = Modifier.padding(top = 0.dp), onClick = {
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

private fun setFullscreenMode(fullscreenMode: FullscreenMode, activity: Activity) {
    when (fullscreenMode) {
        // FullscreenMode.Normal -> normalSystemUI()
        FullscreenMode.TranslucentNavigation -> activity.translucentSystemUI()
        FullscreenMode.Immersive -> activity.immersiveSystemUI()
    }
}

@Composable
private fun TestModeView(isTestMode: State<Boolean>) {
    if (isTestMode.value) {
        Body1Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .background(AppColors.Red, RoundedCornerShape(40.dp))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            text = "Test mode"
        )
    }
}

internal enum class MainScreenState {
    NotInitialized,
    Initializing,
    Initialized,
}