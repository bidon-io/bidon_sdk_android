package com.appodeal.mads

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.appodeal.mads.navigation.NavigationGraph
import com.appodeal.mads.theme.AppTheme
import com.ironsource.mediationsdk.IronSource

class IronSourceActivity : FragmentActivity() {
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
    }

    override fun onResume() {
        super.onResume()
        IronSource.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        IronSource.onPause(this)
    }
}