package org.bidon.admob.impl

import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            val bidType = adUnit.bidType
            when (adType) {
                AdType.Banner -> {
                    if (bidType == BidType.RTB) {
                        AdmobBannerAuctionParams.Bidding(
                            activity = activity,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            adUnit = adUnit,
                        )
                    } else {
                        AdmobBannerAuctionParams.Network(
                            activity = activity,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            adUnit = adUnit,
                        )
                    }
                }

                AdType.Interstitial,
                AdType.Rewarded -> {
                    if (bidType == BidType.RTB) {
                        AdmobFullscreenAdAuctionParams.Bidding(
                            activity = activity,
                            adUnit = adUnit,
                        )
                    } else {
                        AdmobFullscreenAdAuctionParams.Network(
                            activity = activity,
                            adUnit = adUnit,
                        )
                    }
                }
            }
        }
    }
}