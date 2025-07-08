package org.bidon.demoapp.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.chartboost.heliumsdk.domain.AdFormat
import com.chartboost.mediation.googlebiddingadapter.DeleteMe
import org.bidon.demoapp.BuildConfig
import org.bidon.demoapp.component.AppOutlinedButton
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.ItemSelector
import org.bidon.demoapp.component.Subtitle1Text
import org.bidon.demoapp.ui.ext.LocalDateTimeNow
import org.bidon.demoapp.ui.model.SdkStateViewModel
import org.bidon.demoapp.ui.settings.SegmentSettingsView
import org.bidon.demoapp.ui.settings.TestModeInfo
import org.bidon.demoapp.ui.settings.data.Host
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.config.DefaultAdapters
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
@Composable
fun SdkSettings(sdkStateViewModel: SdkStateViewModel) {
    val tempAdmobBid = DeleteMe.payloadFlow.collectAsState().value
    val isInitialized by sdkStateViewModel.isInitialized.collectAsState()
    val buttonText = if (isInitialized) "SDK Initialized" else "Initialize SDK"
    val shared = LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE)
    val activity = LocalContext.current as Activity
    BidonSdk.setLoggerLevel(Logger.Level.Verbose)
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        val testModeState = remember {
            mutableStateOf(shared.getBoolean(TestModeKey, false))
        }
        val coppaState = remember {
            mutableStateOf(
                shared.getInt("coppa", Coppa.Default.code).let { code ->
                    Coppa.values().first { it.code == code }
                }
            )
        }
        val gdprState = remember {
            mutableStateOf(
                shared.getInt("gdpr", Gdpr.Default.code).let { code ->
                    Gdpr.values().first { it.code == code }
                }
            )
        }
        Subtitle1Text(text = "Intercept Chartboost payload (adm)")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.horizontalScroll(rememberScrollState())) {
            AppTextButton(
                modifier = Modifier.background(
                    if (tempAdmobBid is DeleteMe.AdType.Interstitial) Color.Green else Color.Transparent
                ),
                text = "Interstitial",
            ) {
                ChartBoo.st(AdFormat.INTERSTITIAL, activity)
            }
            AppTextButton(
                modifier = Modifier.background(
                    if (tempAdmobBid is DeleteMe.AdType.Rewarded) Color.Green else Color.Transparent
                ),
                text = "Rewarded",
            ) {
                ChartBoo.st(AdFormat.REWARDED, activity)
            }
            AppTextButton(
                modifier = Modifier.background(
                    if (tempAdmobBid is DeleteMe.AdType.Banner) Color.Green else Color.Transparent
                ),
                text = "Banner",
            ) {
                ChartBoo.st(AdFormat.BANNER, activity)
            }
        }
        Subtitle1Text(text = "Bidon SDK settings")
        AppOutlinedButton(
            modifier = Modifier.padding(top = 16.dp),
            text = "Add SDK-level Extras"
        ) {
            BidonSdk.addExtra("token_json", JSONObject("""{"a":"sdk_level"}"""))
            BidonSdk.addExtra("sdk_level_long", LocalDateTimeNow)
        }
        SegmentSettingsView()
        ItemSelector(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.Start,
            title = "Test mode",
            items = listOf(true, false),
            selectedItem = testModeState.value,
            getItemTitle = { testMode ->
                "True".takeIf { testMode } ?: "False"
            },
            onItemClicked = { testMode ->
                shared.edit {
                    putBoolean(TestModeKey, testMode)
                }
                TestModeInfo.isTesMode.value = testMode
                testModeState.value = testMode
                BidonSdk.setTestMode(testMode)
            }
        )
        ItemSelector(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.Start,
            title = "COPPA",
            items = Coppa.values().toList(),
            selectedItem = coppaState.value,
            getItemTitle = { coppa: Coppa ->
                "$coppa"
            },
            onItemClicked = { coppa ->
                shared.edit {
                    putInt("coppa", coppa.code)
                }
                coppaState.value = coppa
                BidonSdk.regulation.coppa = coppa
            }
        )
        ItemSelector(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.Start,
            title = "GDPR",
            items = Gdpr.values().toList(),
            selectedItem = gdprState.value,
            getItemTitle = { gdpr ->
                "$gdpr"
            },
            onItemClicked = { gdpr ->
                shared.edit {
                    putInt("gdpr", gdpr.code)
                }
                gdprState.value = gdpr
                BidonSdk.regulation.gdpr = gdpr
                BidonSdk.regulation.gdprConsentString = "CQEu3gAQEu3gABGACAENBGFMAP_gAEPgAAAAKaNV_G__bWlr8X73aftkeY1P9_h77sQxBhfJE-4FzLvW_JwXx2ExNA36tqIKmRIAu3bBIQNlGJDUTVCgaogVryDMak2coTNKJ6BkiFMRO2dYCF5vm4tj-QKY5vr991dx2B-t7dr83dzyz4VHn3a5_2a0WJCdA5-tDfv9bROb-9IOd_x8v4v8_F_rE2_eT1l_tevp7D9-cts7_XW-9_fff_9Ln_-uB_-_wU1AJMNCogDLIkJCDQMIIEAKgrCAigQAAAAkDRAQAmDAp2BgEusJEAIAUAAwQAgABRkACAAASABCIAIACgQAAQCBQABgAQDAQAMDAAGACwEAgABAdAxTAggUCwASMyIhTAhCASCAlsqEEgCBBXCEIs8CiAREwUAAAJABWAAICwWBxJICViQQJcQbQAAEACAQQAVCKTswBBAGbLUXiybRlaYFo-YLntMAyQAA.IKatV_G__bXlv-X736ftkeY1f9_h77sQxBhfJs-4FzLvW_JwX32EzNE36tqYKmRIAu3bBIQNtGJjUTVChaogVrzDsak2coTtKJ-BkiHMRe2dYCF5vm4tj-QKZ5vr_91d52R_t7dr-3dzyz5Vnv3a9_-b1WJidK5-tH_v_bROb-_I-9_x-_4v8_N_rE2_eT1t_tevt739-8tv___f_9___________3_-_4AA"
            }
        )
        AppOutlinedButton(
            modifier = Modifier.padding(top = 16.dp),
            text = buttonText,
            enabled = !isInitialized
        ) {
            val host = Host.fromString(shared.getString("host", null))
            BidonSdk
                .apply {
                    DefaultAdapters.entries.forEach {
                        registerAdapter(it.classPath)
                    }
                }
                .setBaseUrl(host.baseUrl)
                .setInitializationCallback {
                    sdkStateViewModel.notifySdkInitialized()
                }
                .initialize(
                    context = activity,
                    appKey = BuildConfig.BIDON_API_KEY,
                )
        }
    }
}

internal const val TestModeKey = "test_mode"