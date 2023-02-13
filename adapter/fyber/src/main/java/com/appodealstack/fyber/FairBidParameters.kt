package com.appodealstack.fyber

import android.view.ViewGroup
import com.appodealstack.bidon.adapter.AdAuctionParams
import com.appodealstack.bidon.adapter.AdapterParameters

data class FairBidParameters(
    val appKey: String,
) : AdapterParameters

data class FairBidBannerAuctionParams(
    val adContainer: ViewGroup
) : AdAuctionParams
