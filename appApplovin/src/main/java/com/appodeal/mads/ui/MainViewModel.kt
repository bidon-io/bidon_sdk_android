package com.appodeal.mads.ui

import android.app.Activity
import com.appodealstack.admob.AdmobAdapter
import com.appodealstack.admob.AdmobParameters
import com.appodealstack.applovin.AppLovinDecorator
import com.appodealstack.appsflyer.AppsflyerAnalytics
import com.appodealstack.appsflyer.AppsflyerParameters
import com.appodealstack.bidmachine.BidMachineAdapter
import com.appodealstack.bidmachine.BidMachineParameters

internal class MainViewModel {
    fun initSdk(
        activity: Activity,
        onInitialized: () -> Unit
    ) {
        AppLovinDecorator.getInstance(activity).mediationProvider = "max"
        AppLovinDecorator
            .register(
                AppsflyerAnalytics::class.java,
                AppsflyerParameters("XXXXXXXXXXXXX", "---")
            )
            .register(
                AdmobAdapter::class.java,
                AdmobParameters(
                    interstitials = mapOf(
                        0.1 to "ca-app-pub-3940256099942544/1033173712",
                        1.0 to "ca-app-pub-3940256099942544/1033173712",
                        2.0 to "ca-app-pub-3940256099942544/1033173712",
                    ),
                    rewarded = mapOf(
                        0.1 to "ca-app-pub-3940256099942544/5224354917",
                        1.0 to "ca-app-pub-3940256099942544/5224354917",
                        2.0 to "ca-app-pub-3940256099942544/5224354917",
                    ),
                    banners = mapOf(
                        0.1 to "ca-app-pub-3940256099942544/6300978111",
                        1.0 to "ca-app-pub-3940256099942544/6300978111",
                        2.0 to "ca-app-pub-3940256099942544/6300978111",
                    ),
                )
            )
            .register(
                BidMachineAdapter::class.java,
                BidMachineParameters(sourceId = "1")
            )
            .initializeSdk(activity) {
                onInitialized()
            }
    }
}