package org.bidon.gam.impl

import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamDemandId
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.models.BidType

internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType,
        bidType: BidType
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            when (adType) {
                AdType.Banner -> {
                    if (bidType == BidType.RTB) {
                        GamBannerAuctionParams.Bidding(
                            activity = activity,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            price = pricefloor,
                            bidResponse = requiredBidResponse,
                        )
                    } else {
                        GamBannerAuctionParams.Network(
                            adUnit = popAdUnit(GamDemandId, bidType) ?: error(BidonError.NoAppropriateAdUnitId),
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
                            price = pricefloor,
                            bidResponse = requiredBidResponse,
                        )
                    } else {
                        GamFullscreenAdAuctionParams.Network(
                            adUnit = popAdUnit(GamDemandId, bidType) ?: error(BidonError.NoAppropriateAdUnitId),
                            activity = activity,
                        )
                    }
                }
            }
        }
    }
}