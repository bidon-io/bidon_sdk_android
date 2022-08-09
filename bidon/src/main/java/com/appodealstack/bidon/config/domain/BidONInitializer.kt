package com.appodealstack.bidon.config.domain

import android.app.Activity

interface BidONInitializer {
    suspend fun init(activity: Activity, appKey: String)
}