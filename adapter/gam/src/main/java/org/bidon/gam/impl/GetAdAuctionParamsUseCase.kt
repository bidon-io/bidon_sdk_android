package org.bidon.gam.impl

import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType

internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            when (adType) {
                AdType.Banner -> {
                    GamBannerAuctionParams.Network(
                        adUnit = adUnit,
                        bannerFormat = bannerFormat,
                        activity = activity,
                        containerWidth = containerWidth,
                    )
                }

                AdType.Interstitial,
                AdType.Rewarded -> {
                    GamFullscreenAdAuctionParams.Network(
                        adUnit = adUnit,
                        activity = activity,
                    )
                }
            }
        }
    }
}