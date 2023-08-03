package org.bidon.demoapp.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppToolbar
import org.bidon.demoapp.component.Body1Text
import org.bidon.demoapp.component.CaptionText
import org.bidon.demoapp.theme.getShapeByPositionFor
import org.bidon.demoapp.ui.settings.data.Host

private const val MockUrl = "https://ef5347ef-7389-4095-8a57-cc78c827f8b2.mock.pstmn.io"
internal const val AppBaqendBaseUrl = "https://b.appbaqend.com"

@Composable
internal fun ServerSettingsScreen(
    navController: NavController,
    sharedPreferences: SharedPreferences
) {
    val host = remember {
        mutableStateOf(
            when (sharedPreferences.getString("host", "")) {
                AppBaqendBaseUrl -> Host.Production
                MockUrl -> Host.MockServer
                else -> Host.Production
            }
        )
    }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        AppToolbar(
            title = "Server settings",
            onNavigationButtonClicked = {
                navController.popBackStack()
            }
        )

        RenderHosts(
            hosts = Host.values().toList(),
            selectedHost = host.value,
            onClick = {
                host.value = it
            }
        )
//        RenderPorts(
//            ports = Ports,
//            selectedPort = port.value,
//            onClick = {
//                port.value = it
//            }
//        )
        AppButton(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
            onClick = {
                val hostValue = when (host.value) {
                    Host.Production -> AppBaqendBaseUrl
                    Host.MockServer -> MockUrl
                }
                sharedPreferences.edit().putString("host", hostValue).apply()
                navController.popBackStack()
            },
            text = "Apply"
        )
    }
}

@Composable
private fun RenderHosts(hosts: List<Host>, selectedHost: Host, onClick: (Host) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(fraction = 1f)
    ) {
        CaptionText(text = "Host")
        hosts.forEach { host ->
            Box(
                modifier = Modifier
                    .padding(
                        bottom = 1.dp
                    )
                    .clickable {
                        onClick.invoke(host)
                    }
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = hosts.getShapeByPositionFor(host)
                    )
                    .padding(
                        horizontal = 10.dp,
                    )

            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Body1Text(
                        text = host.name,
                    )
                    CaptionText(
                        text = when (host) {
                            Host.Production -> AppBaqendBaseUrl
                            Host.MockServer -> MockUrl
                        },
                        maxLines = 1
                    )
                }
                if (host == selectedHost) {
                    Image(
                        painter = rememberVectorPainter(Icons.Outlined.Check),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        contentDescription = "",
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}
