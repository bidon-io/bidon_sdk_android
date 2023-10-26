package org.bidon.mobilefuse.impl

import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 */
internal class ObtainAuctionParamUseCase {
    fun getFullscreenParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MobileFuseFullscreenAuctionParams(
                activity = activity,
                signalData = getSignalData(),
                price = pricefloor,
                placementId = getPlacementId()
            )
        }
    }

    fun getBannerParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return auctionParamsScope {
            MobileFuseBannerAuctionParams(
                activity = activity,
                signalData = getSignalData(),
                price = pricefloor,
                bannerFormat = bannerFormat,
                placementId = getPlacementId()
            )
        }
    }

    private fun AdAuctionParamSource.getPlacementId() = requireNotNull(json?.getString("placement_id")) {
        "PlacementId is required for MobileFuse"
    }

    private fun AdAuctionParamSource.getSignalData() = requireNotNull(json?.getString("signaldata")) {
        "SignalData is required for MobileFuse"
    }
}