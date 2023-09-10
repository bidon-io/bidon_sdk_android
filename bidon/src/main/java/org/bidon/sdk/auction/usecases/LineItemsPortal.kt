package org.bidon.sdk.auction.usecases

import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 10/09/2023.
 */
object LineItemsPortal {
    val interstitialLineItems = mutableListOf<LineItem>()
    val bannerLineItems = mutableListOf<LineItem>()
}