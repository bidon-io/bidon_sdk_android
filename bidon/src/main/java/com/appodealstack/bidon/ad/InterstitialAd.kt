package com.appodealstack.bidon.ad

import com.appodealstack.bidon.BidOnSdk.Companion.DefaultPlacement
import com.appodealstack.bidon.adapters.AdListener
import com.appodealstack.bidon.auctions.domain.NewAuctionListener
import com.appodealstack.bidon.auctions.domain.RoundsListener

class InterstitialAd(
    override val placementId: String = DefaultPlacement
) : Interstitial by InterstitialImpl(placementId)

interface Interstitial {
    val placementId: String

    fun load(placement: String? = null)
    fun show(placement: String? = null)
    fun setInterstitialCallback(callback: InterstitialCallback)
}

internal class InterstitialImpl(override val placementId: String) : Interstitial {
    override fun load(placement: String?) {
    }

    override fun show(placement: String?) {
    }

    override fun setInterstitialCallback(callback: InterstitialCallback) {
    }
}

interface InterstitialCallback : AdListener, NewAuctionListener, RoundsListener