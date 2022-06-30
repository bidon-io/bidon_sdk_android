package com.appodealstack.mads.postbid

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.base.AdType
import com.appodealstack.mads.demands.Demand
import com.appodealstack.mads.demands.DemandId

class BidMachineDemand : Demand {
    override val demandId = DemandId("BidMachine")

    override suspend fun init(context: Context, configParams: Bundle) {
        TODO("Not yet implemented")
    }

    override fun loadAd(activity: Activity, adType: AdType, adParams: Bundle) {
        TODO("Not yet implemented")
    }

    override fun showAd(adType: AdType, bundle: Bundle): Boolean {
        TODO("Not yet implemented")
    }

    override fun destroyAd(adType: AdType, bundle: Bundle): Boolean {
        TODO("Not yet implemented")
    }

    override fun setExtras(bundle: Bundle) {
        TODO("Not yet implemented")
    }

    override fun canShow(adType: AdType, adParams: Bundle): Boolean {
        TODO("Not yet implemented")
    }
}