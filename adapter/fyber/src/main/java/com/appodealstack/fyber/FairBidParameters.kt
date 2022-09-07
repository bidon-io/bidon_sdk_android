package com.appodealstack.fyber

import android.view.ViewGroup
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdapterParameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FairBidParameters(
    @SerialName("app_key")
    val appKey: String,
) : AdapterParameters

data class FairBidBannerAuctionParams(
    val adContainer: ViewGroup
) : AdAuctionParams
