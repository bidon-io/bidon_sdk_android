package com.appodealstack.ironsource

import com.ironsource.mediationsdk.impressionData.ImpressionData

interface BNIronSourceImpressionDataListener {
    fun onImpressionSuccess(impressionData: ImpressionData?)
}

