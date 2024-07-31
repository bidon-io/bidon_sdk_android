package org.bidon.mobilefuse.impl

import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 */
internal class ObtainAuctionParamUseCase {
    fun getFullscreenParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MobileFuseFullscreenAuctionParams(
                activity = activity,
                bidResponse = requiredBidResponse
            )
        }
    }

    fun getBannerParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MobileFuseBannerAuctionParams(
                activity = activity,
                bannerFormat = bannerFormat,
                bidResponse = requiredBidResponse
            )
        }
    }
}