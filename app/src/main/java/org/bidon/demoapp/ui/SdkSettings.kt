package org.bidon.demoapp.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.bidon.demoapp.component.AppOutlinedButton
import org.bidon.demoapp.component.ItemSelector
import org.bidon.demoapp.component.Subtitle1Text
import org.bidon.demoapp.ui.settings.TestModeInfo
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.segment.models.Gender
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by Aleksei Cherniaev on 21/06/2023.
 */
@Composable
fun SdkSettings() {
    val shared = LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE)
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
        Subtitle1Text(text = "Bidon SDK settings")
        AppOutlinedButton(
            modifier = Modifier.padding(top = 16.dp),
            text = "Add SDK-level Extras"
        ) {
            BidonSdk.addExtra("token_json", JSONObject("""{"a":"sdk_level"}"""))
            BidonSdk.addExtra("sdk_level_long", LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
        }
        SegmentAttrButton()
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

@Composable
private fun SegmentAttrButton() {
    AppOutlinedButton(
        modifier = Modifier.padding(top = 0.dp),
        text = "Add Segment attrs"
    ) {
        BidonSdk.segment.setAge(18)
        BidonSdk.segment.setGender(Gender.Female)
        BidonSdk.segment.setLevel(100500)
        BidonSdk.segment.setPaying(isPaying = true)
        BidonSdk.segment.setInAppAmount(15)
        BidonSdk.segment.setCustomAttributes(mapOf("attr1" to "hello world"))
        BidonSdk.segment.putCustomAttribute(attribute = "attr2", value = 28)
    }
}

internal const val TestModeKey = "test_mode"