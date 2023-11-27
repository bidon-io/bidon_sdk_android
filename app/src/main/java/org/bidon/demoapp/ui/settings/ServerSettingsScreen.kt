package org.bidon.demoapp.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.bidon.demoapp.component.AppButton
import org.bidon.demoapp.component.AppToolbar
import org.bidon.demoapp.component.Body1Text
import org.bidon.demoapp.component.CaptionText
import org.bidon.demoapp.theme.AppTypography
import org.bidon.demoapp.theme.getShapeByPositionFor
import org.bidon.demoapp.ui.settings.data.Host

@Composable
internal fun ServerSettingsScreen(
    navController: NavController,
    sharedPreferences: SharedPreferences
) {
    val state = remember {
        mutableStateOf(
            Host.values(
                savedString = sharedPreferences.getString("host", null)
            )
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
            hosts = state.value.hosts,
            selected = state.value.selected,
            onClick = { newHost ->
                state.value = state.value.copy(
                    selected = newHost,
                )
            },
            onCustomHostChanged = { newHost ->
                state.value = state.value.copy(
                    hosts = state.value.hosts.map { currentHost ->
                        if (currentHost is Host.Staging) {
                            newHost
                        } else {
                            currentHost
                        }
                    },
                    selected = newHost
                )
            }
        )
        AppButton(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally),
            onClick = {
                sharedPreferences.edit().putString("host", state.value.selected.baseUrl).apply()
                navController.popBackStack()
            },
            text = "Apply"
        )
    }
}

@Composable
private fun RenderHosts(
    hosts: List<Host>,
    selected: Host,
    onClick: (Host) -> Unit,
    onCustomHostChanged: (Host) -> Unit
) {
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
                    when (host) {
                        Host.Production -> {
                            CaptionText(
                                text = host.baseUrl,
                                maxLines = 1
                            )
                        }

                        Host.MockServer -> {
                            CaptionText(
                                text = host.baseUrl,
                                maxLines = 1
                            )
                        }

                        is Host.Staging -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CaptionText(
                                    text = Host.Staging.SCHEME,
                                    maxLines = 1
                                )
                                BasicTextField(
                                    value = host.prefix,
                                    onValueChange = { newValue ->
                                        onCustomHostChanged(Host.Staging(newValue))
                                    },
                                    textStyle = AppTypography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        background = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    maxLines = 1,
                                )
                                CaptionText(
                                    text = Host.Staging.SUFFIX,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                if (selected == host) {
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
