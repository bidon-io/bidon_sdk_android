package org.bidon.sdk.auction.ext

import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.logs.logging.impl.logInfo

internal fun AuctionResponse.printWaterfall(adType: AdType) {
    adUnits?.joinToString(separator = "\n") { adUnit ->
        "#${adUnits.indexOf(adUnit)} $adUnit"
    }?.let {
        logInfo("$adType auction waterfall", "\n$it")
    }
}