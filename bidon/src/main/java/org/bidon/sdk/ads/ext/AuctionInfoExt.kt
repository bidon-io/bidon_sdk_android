package org.bidon.sdk.ads.ext

import org.bidon.sdk.ads.AdUnitInfo
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsAdUnit

internal fun StatsAdUnit.toAuctionInfo() =
    AdUnitInfo(
        demandId = demandId,
        label = adUnitLabel,
        price = price,
        uid = adUnitUid,
        bidType = bidType,
        fillStartTs = fillStartTs,
        fillFinishTs = fillFinishTs,
        status = status,
        ext = ext.toString(),
    )

internal fun AdUnit.toAuctionNoBidInfo() =
    AdUnitInfo(
        demandId = demandId,
        label = label,
        price = pricefloor,
        uid = uid,
        bidType = BidType.RTB.code,
        fillStartTs = null,
        fillFinishTs = null,
        status = RoundStatus.NoBid.code,
        ext = extra.toString(),
    )