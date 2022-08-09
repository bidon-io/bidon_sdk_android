package com.appodealstack.bidon.config.data

import android.app.Activity
import com.appodealstack.bidon.config.domain.AdapterRegister
import com.appodealstack.bidon.config.domain.BidONInitializer

internal class BidONInitializerImpl(
    private val adapterRegister: AdapterRegister
): BidONInitializer {
    override suspend fun init(activity: Activity, appKey: String) {
        //val adapters = get

        TODO("Not yet implemented")
    }
}