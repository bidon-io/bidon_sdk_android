package org.bidon.gam.impl

import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.stats.models.BidType

internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            val bidType = auctionParamsScope.adUnit.bidType
            when (adType) {
                AdType.Banner -> {
                    if (bidType == BidType.RTB) {
                        GamBannerAuctionParams.Bidding(
                            activity = activity,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            adUnit = adUnit,
                        )
                    } else {
                        GamBannerAuctionParams.Network(
                            adUnit = adUnit,
                            bannerFormat = bannerFormat,
                            activity = activity,
                            containerWidth = containerWidth,
                        )
                    }
                }

                AdType.Interstitial,
                AdType.Rewarded -> {
                    if (bidType == BidType.RTB) {
                        GamFullscreenAdAuctionParams.Bidding(
                            activity = activity,
                            adUnit = adUnit,
                        )
                    } else {
                        GamFullscreenAdAuctionParams.Network(
                            adUnit = adUnit,
                            activity = activity,
                        )
                    }
                }
            }
        }
    }
}