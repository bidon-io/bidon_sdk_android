package com.appodealstack.admob

import android.content.Context
import android.view.ViewGroup
import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.domain.adapter.AdAuctionParams
import com.appodealstack.bidon.domain.adapter.AdapterParameters
import com.appodealstack.bidon.domain.common.BannerSize

object AdmobInitParameters : AdapterParameters

data class AdmobBannerAuctionParams(
    val adContainer: ViewGroup,
    val bannerSize: BannerSize,
    val lineItem: LineItem,
    val priceFloor: Double
) : AdAuctionParams

data class AdmobFullscreenAdAuctionParams(
    val context: Context,
    val lineItem: LineItem,
    val priceFloor: Double
) : AdAuctionParams
