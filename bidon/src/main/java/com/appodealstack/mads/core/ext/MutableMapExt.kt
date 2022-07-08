package com.appodealstack.mads.core.ext

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.appodealstack.mads.auctions.AuctionRequest
import com.appodealstack.mads.demands.AdSource
import com.appodealstack.mads.demands.AdType
import com.appodealstack.mads.demands.Adapter
import com.appodealstack.mads.demands.DemandAd

internal fun <K, V> MutableMap<K, V>.addOrRemoveIfNull(key: K, value: V?) {
    if (value != null) {
        this[key] = value
    } else {
        this.remove(key)
    }
}

internal fun List<Adapter>.retrieveAuctionRequests(
    activity: Activity?,
    demandAd: DemandAd,
    adParams: Bundle
): List<AuctionRequest> {
    return this.mapNotNull {
        when (demandAd.adType) {
            AdType.Interstitial -> (it as? AdSource.Interstitial)?.interstitial(activity, demandAd, adParams)
            AdType.Rewarded -> (it as? AdSource.Rewarded)?.rewarded(activity, demandAd, adParams)
            AdType.Banner -> null
        }
    }
}

internal fun List<AdSource.Banner>.retrieveAuctionRequests(
    context: Context,
    demandAd: DemandAd,
    adParams: Bundle
): List<AuctionRequest> {
    require(demandAd.adType == AdType.Banner)
    return this.map {
        it.banner(context, demandAd, adParams)
    }
}