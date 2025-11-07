package org.bidon.taurusx.impl

import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams

internal class ObtainAuctionParamUseCase {
    fun getFullscreenParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            TaurusXFullscreenAuctionParams(
                context = activity.applicationContext,
                adUnit = adUnit
            )
        }
    }

    fun getBannerParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            TaurusXBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }
}