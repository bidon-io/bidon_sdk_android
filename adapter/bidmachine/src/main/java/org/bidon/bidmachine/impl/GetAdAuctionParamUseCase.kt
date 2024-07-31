package org.bidon.bidmachine.impl

import org.bidon.bidmachine.BMBannerAuctionParams
import org.bidon.bidmachine.BMFullscreenAuctionParams
import org.bidon.bidmachine.BidMachineDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.stats.models.BidType

/**
 * Created by Aleksei Cherniaev on 21/11/2023.
 */
class GetAdAuctionParamUseCase {
    fun getBMFullscreenAuctionParams(auctionParamsScope: AdAuctionParamSource, bidType: BidType): Result<BMFullscreenAuctionParams> {
        return auctionParamsScope {
            BMFullscreenAuctionParams(
                price = when (bidType) {
                    BidType.RTB -> requiredBidResponse.price
                    BidType.CPM -> pricefloor
                },
                timeout = timeout,
                context = activity.applicationContext,
                adUnit = when (bidType) {
                    BidType.RTB -> requiredBidResponse.adUnit
                    BidType.CPM -> popAdUnit(BidMachineDemandId, bidType) ?: error(BidonError.NoAppropriateAdUnitId)
                },
                payload = if (bidType == BidType.RTB) {
                    requireNotNull(requiredBidResponse.extra?.getString("payload")) {
                        "No payload found in bid response"
                    }
                } else null
            )
        }
    }

    fun getBMBannerAuctionParams(auctionParamsScope: AdAuctionParamSource, bidType: BidType): Result<BMBannerAuctionParams> {
        return auctionParamsScope {
            BMBannerAuctionParams(
                price = when (bidType) {
                    BidType.RTB -> requiredBidResponse.price
                    BidType.CPM -> pricefloor
                },
                timeout = timeout,
                activity = activity,
                bannerFormat = bannerFormat,
                adUnit = when (bidType) {
                    BidType.RTB -> requiredBidResponse.adUnit
                    BidType.CPM -> popAdUnit(BidMachineDemandId, bidType) ?: error(BidonError.NoAppropriateAdUnitId)
                },
                payload = if (bidType == BidType.RTB) {
                    requireNotNull(requiredBidResponse.extra?.getString("payload")) {
                        "No payload found in bid response"
                    }
                } else null
            )
        }
    }
}