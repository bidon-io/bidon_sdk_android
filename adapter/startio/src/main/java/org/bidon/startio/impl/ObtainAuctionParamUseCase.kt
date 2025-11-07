package org.bidon.startio.impl

import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams

internal class ObtainAuctionParamUseCase {
    fun getFullscreenParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            StartIoFullscreenAuctionParams(
                context = auctionParamsScope.activity.applicationContext,
                adUnit = adUnit
            )
        }
    }

    fun getBannerParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            StartIoBannerAuctionParams(
                activity = auctionParamsScope.activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }
}
