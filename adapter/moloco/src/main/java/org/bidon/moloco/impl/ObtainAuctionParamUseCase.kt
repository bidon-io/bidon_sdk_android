package org.bidon.moloco.impl

import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams

internal class ObtainAuctionParamUseCase {
    fun getFullscreenParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MolocoFullscreenAuctionParams(
                adUnit = adUnit
            )
        }
    }

    fun getBannerParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MolocoBannerAuctionParams(
                bannerFormat = bannerFormat,
                adUnit = adUnit
            )
        }
    }
}
