package org.bidon.demoapp

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import org.bidon.demoapp.navigation.NavigationGraph
import org.bidon.demoapp.theme.AppTheme
import org.bidon.demoapp.ui.SdkSettings
import org.bidon.demoapp.ui.TestModeKey
import org.bidon.demoapp.ui.settings.TestModeInfo

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded },
                skipHalfExpanded = true,
            )

            AppTheme {
                val navController = rememberNavController()
                LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE).let {
                    TestModeInfo.isTesMode.value = it.getBoolean(TestModeKey, false)
                }

                ModalBottomSheetLayout(
                    sheetState = modalSheetState,
                    sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    sheetContent = {
                        SdkSettings()
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        Column {
                            NavigationGraph(
                                navController = navController,
                            )
                        }
                        FloatingActionButton(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp),
                            onClick = {
                                coroutineScope.launch { modalSheetState.show() }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Pets,
                                contentDescription = "Add FAB",
                                tint = Color.White,
                            )
                        }
                    }
                }
            }

            val permissionsState = rememberMultiplePermissionsState(
                permissions = listOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            LaunchedEffect(key1 = Unit, block = {
                permissionsState.permissions
                    .firstOrNull { !it.status.isGranted }
                    ?.launchPermissionRequest()
            })
        }
    }
}
