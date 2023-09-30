package org.bidon.sdk.auction.models

import kotlin.test.Test

/**
 * Created by Aleksei Cherniaev on 26/09/2023.
 */
class AdItemTest {

    @Test
    fun sum() {
        val adItem = AdItem(
            lineItem = LineItem(
                uid = "uid",
                demandId = "demandId",
                pricefloor = 0.01,
                adUnitId = "adUnitId"
            ),
        )
        adItem.loaded(true)
        adItem.loaded(false)
        adItem.loaded(false)
        adItem.loaded(true)
        adItem.loaded(false)
        adItem.loaded(false)
        adItem.loaded(false)
        adItem.loaded(false)
        adItem.loaded(true)

        println(adItem.weight())
        error("Not implemented")
    }

    @Test
    fun sum2() {
        val adItem = AdItem(
            lineItem = LineItem(
                uid = "uid",
                demandId = "demandId",
                pricefloor = 0.01,
                adUnitId = "adUnitId"
            )
        )
        adItem.loaded(false)
        adItem.loaded(true)
        adItem.loaded(true)

        println(adItem.weight())
        error("Not implemented")
    }
}