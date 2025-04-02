package org.bidon.admob.impl

import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
internal class GetAdAuctionParamsUseCase {
    operator fun invoke(
        auctionParamsScope: AdAuctionParamSource,
        adType: AdType
    ): Result<AdAuctionParams> {
        return auctionParamsScope {
            when (adType) {
                AdType.Banner -> {
                    AdmobBannerAuctionParams.Network(
                        activity = activity,
                        bannerFormat = bannerFormat,
                        containerWidth = containerWidth,
                        adUnit = adUnit,
                    )
                }

                AdType.Interstitial, AdType.Rewarded -> {
                    AdmobFullscreenAdAuctionParams.Network(
                        activity = activity,
                        adUnit = adUnit,
                    )
                }
            }
        }
    }
}