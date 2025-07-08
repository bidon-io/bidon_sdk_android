package com.applovin.mediation.adapters.keeper

import org.bidon.sdk.ads.Ad

internal interface AdInstance {
    val ecpm: Double
    val uid: String
    val bidType: String
    val demandId: String
    val isReady: Boolean
    fun applyAdInfo(ad: Ad): AdInstance
    fun notifyLoss(winnerDemandId: String, winnerPrice: Double)
    fun destroy()
}

internal const val DEFAULT_ECPM: Double = 0.0
internal const val DEFAULT_DEMAND_ID: String = "error_demand_id"
internal const val DEFAULT_BID_TYPE: String = "undefined"
internal const val DEFAULT_UID: String = "undefined"
