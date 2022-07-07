package com.appodealstack.applovin.banner

import android.content.Context
import com.applovin.mediation.MaxAdFormat
import com.appodealstack.applovin.ext.BNMaxAdViewAdListener
import com.appodealstack.applovin.ext.MaxAdViewWrapperImpl
import com.appodealstack.mads.demands.AdRevenueListener

class BNMaxAdView(
    adUnitId: String,
    context: Context
): MaxAdViewWrapper by MaxAdViewWrapperImpl(adUnitId, context)

internal interface MaxAdViewWrapper {
    fun setListener(listener: BNMaxAdViewAdListener)
    fun setRevenueListener(listener: AdRevenueListener)
    fun loadAd()
    fun setBackgroundColor(color: Int)
    fun setAlpha(alpha: Float)
    fun destroy()
    fun getAdUnitId(): String
    fun getAdFormat(): MaxAdFormat
    fun setCustomData(value: String)
    fun setExtraParameter(key: String, value: String)
    fun startAutoRefresh()
    fun stopAutoRefresh()
    fun setPlacement(placement: String?)
    fun getPlacement(): String?
}