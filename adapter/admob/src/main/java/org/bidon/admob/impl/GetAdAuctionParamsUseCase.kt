package org.bidon.admob.impl

import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobDemandId
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.config.BidonError

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
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
                        AdmobBannerAuctionParams.Bidding(
                            context = activity.applicationContext,
                            bannerFormat = bannerFormat,
                            containerWidth = containerWidth,
                            price = pricefloor,
                            adUnitId = requireNotNull(json?.getString("ad_unit_id")),
                            payload = requireNotNull(json?.getString("payload"))
                        )
                    } else {
                        AdmobBannerAuctionParams.Network(
                            lineItem = popLineItem(AdmobDemandId) ?: error(BidonError.NoAppropriateAdUnitId),
                            bannerFormat = bannerFormat,
                            context = activity.applicationContext,
                            containerWidth = containerWidth,
                        )
                    }
                }

                AdType.Interstitial,
                AdType.Rewarded -> {
                    if (isBiddingMode) {
                        AdmobFullscreenAdAuctionParams.Bidding(
                            context = activity.applicationContext,
                            price = pricefloor,
                            adUnitId = requireNotNull(json?.getString("ad_unit_id")),
                            payload = requireNotNull(json?.getString("payload"))
                        )
                    } else {
                        AdmobFullscreenAdAuctionParams.Network(
                            lineItem = popLineItem(AdmobDemandId) ?: error(BidonError.NoAppropriateAdUnitId),
                            context = activity.applicationContext,
                        )
                    }
                }
            }
        }
    }
}