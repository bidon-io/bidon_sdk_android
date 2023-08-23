package org.bidon.demoapp.ui

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import org.bidon.demoapp.component.AppOutlinedButton
import org.bidon.demoapp.component.AppTextButton
import org.bidon.demoapp.component.ItemSelector
import org.bidon.demoapp.component.Subtitle1Text
import org.bidon.demoapp.ui.ext.LocalDateTimeNow
import org.bidon.demoapp.ui.settings.SegmentSettingsView
import org.bidon.demoapp.ui.settings.TestModeInfo
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
@Composable
fun SdkSettings() {
    val tempAdmobBid = DeleteMe.payloadFlow.collectAsState().value
    val shared = LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE)
    val activity = LocalContext.current as Activity
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
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
            }
        )
    }
}

internal const val TestModeKey = "test_mode"