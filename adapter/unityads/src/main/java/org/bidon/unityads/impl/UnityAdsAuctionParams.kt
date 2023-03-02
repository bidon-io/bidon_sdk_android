package org.bidon.unityads.impl

import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 02/03/2023.
 */
data class UnityAdsAuctionParams(
    val lineItem: LineItem,
    val pricefloor: Double
) : AdAuctionParams