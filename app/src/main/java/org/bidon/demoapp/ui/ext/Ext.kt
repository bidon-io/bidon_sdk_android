package org.bidon.demoapp.ui.ext

import android.os.Build
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by Aleksei Cherniaev on 13/07/2023.
 */
internal val LocalDateTimeNow
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
    } else {
        System.currentTimeMillis()
    }

internal fun Ad.toUiString(): String {
    return "Ad(${demandAd.adType} $networkName/$bidType $price $currencyCode, auctionId=$auctionId, dsp=$dsp)"
}

internal fun AuctionInfo.toUiString(): String {
    return "Auction($auctionId, pricefloor=$auctionPricefloor, adunits=${adUnits?.size}, nobids=${noBids?.size})"
}