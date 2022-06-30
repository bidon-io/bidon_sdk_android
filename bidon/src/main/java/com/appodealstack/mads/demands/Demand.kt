package com.appodealstack.mads.demands

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.base.AdType

interface Demand {
    val demandId: DemandId

    suspend fun init(context: Context, configParams: Bundle)

    fun loadAd(activity: Activity, adType: AdType, adParams: Bundle)
    fun showAd(adType: AdType, bundle: Bundle): Boolean
    fun destroyAd(adType: AdType, bundle: Bundle): Boolean
    fun setExtras(bundle: Bundle)
    fun canShow(adType: AdType, adParams: Bundle): Boolean
}