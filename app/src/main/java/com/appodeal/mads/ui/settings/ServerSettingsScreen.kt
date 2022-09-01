package com.appodeal.mads.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.appodeal.mads.component.*
import com.appodeal.mads.theme.getShapeByPositionFor
import com.appodeal.mads.ui.settings.data.Host
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorage
import com.appodealstack.bidon.utilities.keyvaluestorage.KeyValueStorageImpl
import com.appodealstack.bidon.utilities.network.NetworkSettings

private const val MockUrl = "https://ef5347ef-7389-4095-8a57-cc78c827f8b2.mock.pstmn.io"

@Composable
internal fun ServerSettingsScreen(
    navController: NavController,
    keyValueStorage: KeyValueStorage = KeyValueStorageImpl(navController.context)
) {
    val host = remember {
        mutableStateOf(
            when (keyValueStorage.host) {
                NetworkSettings.BaseBidOnUrl -> Host.Production
                MockUrl -> Host.MockServer
                else -> Host.Production
            }
        )
    }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colors.background)
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
                keyValueStorage.host = when (host.value) {
                    Host.Production -> NetworkSettings.BaseBidOnUrl
                    Host.MockServer -> MockUrl
                }
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
                        color = MaterialTheme.colors.surface,
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
                            Host.Production -> NetworkSettings.BaseBidOnUrl
                            Host.MockServer -> MockUrl
                        },
                        maxLines = 1
                    )
                }
                if (host == selectedHost) {
                    Image(
                        painter = rememberVectorPainter(Icons.Outlined.Check),
                        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
                        contentDescription = "",
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}
