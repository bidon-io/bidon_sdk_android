package org.bidon.bidmachine.impl

import org.bidon.bidmachine.BMBannerAuctionParams
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.sdk.adapter.AdAuctionParamSource

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class GetAdAuctionParamUseCase {
    fun getBMFullscreenAuctionParams(auctionParamsScope: AdAuctionParamSource): Result<BMFullscreenAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                price = adUnit.pricefloor,
                timeout = adUnit.timeout,
                context = activity.applicationContext,
                adUnit = adUnit,
                payload = adUnit.extra?.getString("payload")
            )
        }
    }

    fun getBMBannerAuctionParams(auctionParamsScope: AdAuctionParamSource): Result<BMBannerAuctionParams> {
        return auctionParamsScope {
            BMBannerAuctionParams(
                price = adUnit.pricefloor,
                timeout = adUnit.timeout,
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = adUnit,
                payload = adUnit.extra?.getString("payload")
            )
        }
    }
}