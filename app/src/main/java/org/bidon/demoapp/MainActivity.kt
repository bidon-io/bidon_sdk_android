package org.bidon.demoapp

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import org.bidon.demoapp.navigation.NavigationGraph
import org.bidon.demoapp.theme.AppTheme
import org.bidon.demoapp.ui.SdkSettings
import org.bidon.demoapp.ui.TestModeKey
import org.bidon.demoapp.ui.model.SdkStateViewModel
import org.bidon.demoapp.ui.settings.TestModeInfo

class MainActivity : FragmentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val sdkStateViewModel: SdkStateViewModel =
                viewModel(LocalContext.current as ComponentActivity)
            val coroutineScope = rememberCoroutineScope()
            val modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded },
                skipHalfExpanded = true,
            )

            AppTheme {
                val navController = rememberNavController()
                LocalContext.current.getSharedPreferences("app_test", Context.MODE_PRIVATE).let {
                    TestModeInfo.isTesMode.value = it.getBoolean(TestModeKey, false)
                }

                ModalBottomSheetLayout(
                    modifier = Modifier,
                    sheetState = modalSheetState,
                    sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    sheetContent = {
                        SdkSettings(sdkStateViewModel)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Column {
                            NavigationGraph(
                                navController = navController,
                                sdkStateViewModel = sdkStateViewModel
                            )
                        }
                        FloatingActionButton(
                            modifier = Modifier
                                .safeDrawingPadding()
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
//                        Box(
//                            modifier = Modifier
//                                .size(100.dp)
//                                .background(Color.Red)
//                        )
//                        Box(
//                            modifier = Modifier
//                                .size(100.dp)
//                                .background(Color.Red)
//                                .align(Alignment.BottomEnd)
//                        )
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

@Composable
fun HideSystemBars() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            insetsController.apply {
                show(WindowInsetsCompat.Type.statusBars())
                show(WindowInsetsCompat.Type.navigationBars())
                // systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            }
        }
    }
}
