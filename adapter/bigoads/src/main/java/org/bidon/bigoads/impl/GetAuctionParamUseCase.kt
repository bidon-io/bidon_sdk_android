package org.bidon.bigoads.impl

import org.bidon.bigoads.BigoAdsDemandId
import org.bidon.sdk.adapter.AdAuctionParamSource

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 */
internal class GetAuctionParamUseCase {
    fun getFullscreenParams(
        auctionParamsScope: AdAuctionParamSource,
        isBiddingMode: Boolean
    ): Result<BigoFullscreenAuctionParams> {
        return auctionParamsScope {
            if (isBiddingMode) {
                BigoFullscreenAuctionParams(
                    payload = requireNotNull(json?.optString("payload")) {
                        "Payload is required for Bigo Ads"
                    },
                    slotId = requireNotNull(json?.optString("slot_id")) {
                        "Slot id is required for Bigo Ads"
                    },
                    bidPrice = requireNotNull(json?.optDouble("price")) {
                        "Bid price is required for Bigo Ads"
                    },
                )
            } else {
                val lineItem = popLineItem(BigoAdsDemandId) ?: error("unexpected")
                BigoFullscreenAuctionParams(
                    payload = null,
                    slotId = requireNotNull(lineItem.adUnitId),
                    bidPrice = lineItem.pricefloor
                )
            }
        }
    }

    fun getBannerParams(
        auctionParamsScope: AdAuctionParamSource,
        isBiddingMode: Boolean
    ): Result<BigoBannerAuctionParams> {
        return auctionParamsScope {
            if (isBiddingMode) {
                BigoBannerAuctionParams(
                    bannerFormat = bannerFormat,
                    payload = requireNotNull(json?.optString("payload")) {
                        "Payload is required for BigoAds banner ad"
                    },
                    slotId = requireNotNull(json?.optString("slot_id")) {
                        "Slot id is required for BigoAds banner ad"
                    },
                    bidPrice = requireNotNull(json?.optDouble("price")) {
                        "Bid price is required for BigoAds banner ad"
                    },
                )
            } else {
                val lineItem = popLineItem(BigoAdsDemandId) ?: error("unexpected")
                BigoBannerAuctionParams(
                    bannerFormat = bannerFormat,
                    payload = null,
                    slotId = requireNotNull(lineItem.adUnitId),
                    bidPrice = lineItem.pricefloor
                )
            }
        }
    }
}