package com.appodeal.mads

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.appodeal.mads.navigation.NavigationGraph
import com.appodeal.mads.theme.AppTheme
import com.appodealstack.bidon.TempApi

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                val navController = rememberNavController()
                NavigationGraph(
                    navController = navController,
                )
            }
        }

        // TempApi.start()
    }
}