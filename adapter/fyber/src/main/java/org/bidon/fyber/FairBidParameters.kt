package org.bidon.fyber

import android.view.ViewGroup
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdapterParameters

data class FairBidParameters(
    val appKey: String,
) : AdapterParameters

data class FairBidBannerAuctionParams(
    val adContainer: ViewGroup
) : AdAuctionParams
