package org.bidon.admob.impl

import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobDemandId
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType,
        bidType: BidType
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            when (adType) {
                AdType.Banner -> {
                    val adUnit = popAdUnit(AdmobDemandId, bidType) ?: error(BidonError.NoAppropriateAdUnitId)
                    if (bidType == BidType.RTB) {
                        AdmobBannerAuctionParams.Bidding(
                            activity = activity,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            price = pricefloor,
                            adUnit = adUnit,
                        )
                    } else {
                        AdmobBannerAuctionParams.Network(
                            adUnit = adUnit,
                            bannerFormat = bannerFormat,
                            activity = activity,
                            containerWidth = containerWidth,
                        )
                    }
                }

                AdType.Interstitial,
                AdType.Rewarded -> {
                    val adUnit = popAdUnit(AdmobDemandId, bidType) ?: error(BidonError.NoAppropriateAdUnitId)
                    if (bidType == BidType.RTB) {
                        AdmobFullscreenAdAuctionParams.Bidding(
                            activity = activity,
                            price = pricefloor,
                            adUnit = adUnit,
                        )
                    } else {
                        AdmobFullscreenAdAuctionParams.Network(
                            adUnit = adUnit,
                            activity = activity,
                        )
                    }
                }
            }
        }
    }
}