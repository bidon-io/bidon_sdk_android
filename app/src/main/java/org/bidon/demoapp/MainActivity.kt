package org.bidon.demoapp

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import org.bidon.demoapp.navigation.NavigationGraph
import org.bidon.demoapp.theme.AppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val navController = rememberNavController()
                Column {
                    NavigationGraph(
                        navController = navController,
                        shared = this@MainActivity.getSharedPreferences("app_test", Context.MODE_PRIVATE)
                    )
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