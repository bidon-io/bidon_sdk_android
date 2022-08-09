package com.appodeal.mads.ui

import android.app.Activity
import com.appodealstack.bidon.BidON

internal class MainViewModel {
    fun initSdk(
        activity: Activity,
        onInitialized: () -> Unit
    ) {
        BidON.init(
            activity = activity,
            appKey = "YOUR_APP_KEY",
        ) {
            onInitialized.invoke()
        }
    }
}