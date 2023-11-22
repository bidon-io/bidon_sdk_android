package org.bidon.gam.impl

import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamDemandId
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.config.BidonError

internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType,
        isBiddingMode: Boolean
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            when (adType) {
                AdType.Banner -> {
                    if (isBiddingMode) {
                        GamBannerAuctionParams.Bidding(
                            activity = activity,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            price = pricefloor,
                            adUnitId = requireNotNull(json?.getString("ad_unit_id")),
                            payload = requireNotNull(json?.getString("payload"))
                        )
                    } else {
                        GamBannerAuctionParams.Network(
                            lineItem = popLineItem(GamDemandId) ?: error(BidonError.NoAppropriateAdUnitId),
                            bannerFormat = bannerFormat,
                            activity = activity,
                            containerWidth = containerWidth,
                        )
                    }
                }

                AdType.Interstitial,
                AdType.Rewarded -> {
                    if (isBiddingMode) {
                        GamFullscreenAdAuctionParams.Bidding(
                            activity = activity,
                            price = pricefloor,
                            adUnitId = requireNotNull(json?.getString("ad_unit_id")),
                            payload = requireNotNull(json?.getString("payload"))
                        )
                    } else {
                        GamFullscreenAdAuctionParams.Network(
                            lineItem = popLineItem(GamDemandId) ?: error(BidonError.NoAppropriateAdUnitId),
                            activity = activity,
                        )
                    }
                }
            }
        }
    }
}