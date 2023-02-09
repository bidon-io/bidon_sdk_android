package com.appodealstack.bidon.config.domain

import com.appodealstack.bidon.data.models.auction.LineItem
import com.appodealstack.bidon.data.models.auction.minByPricefloorOrNull
import com.appodealstack.bidon.domain.common.DemandId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LineItemTest {
    @Test
    fun `it should find min LineItem with greater given pricefloor`() {
        val list = listOf(
            LineItem(
                demandId = "demand id123",
                priceFloor = 0.1,
                adUnitId = "unit 0.1"
            ),
            LineItem(
                demandId = null,
                priceFloor = 1.6,
                adUnitId = "unit 1.6"
            ),
            LineItem(
                demandId = "demand id123",
                priceFloor = 2.9,
                adUnitId = "unit 2.9"
            ),
            LineItem(
                demandId = "demand id123",
                priceFloor = 1.5,
                adUnitId = "unit 1.5"
            ),
            LineItem(
                demandId = "demand id123",
                priceFloor = 1.71,
                adUnitId = "unit 1.71"
            ),
            LineItem(
                demandId = "demand id321",
                priceFloor = 1.7,
                adUnitId = "unit 1.7"
            ),
        )
        val result = list.minByPricefloorOrNull(DemandId("demand id123"), 1.5)

        assertThat(result).isEqualTo(
            LineItem(
                demandId = "demand id123",
                priceFloor = 1.71,
                adUnitId = "unit 1.71"
            )
        )
    }

    @Test
    fun `it should NOT find any LineItem with greater given pricefloor`() {
        val list = listOf(
            LineItem(
                demandId = "demand id123",
                priceFloor = 0.1,
                adUnitId = "unit 0.1"
            ),
            LineItem(
                demandId = null,
                priceFloor = 1.6,
                adUnitId = "unit 1.6"
            ),
            LineItem(
                demandId = "demand id123",
                priceFloor = 2.9,
                adUnitId = "unit 2.9"
            ),
            LineItem(
                demandId = "demand id123",
                priceFloor = 1.5,
                adUnitId = "unit 1.5"
            ),
            LineItem(
                demandId = "demand id123",
                priceFloor = 1.71,
                adUnitId = "unit 1.71"
            ),
            LineItem(
                demandId = "demand id321",
                priceFloor = 1.7,
                adUnitId = "unit 1.7"
            ),
        )
        val result = list.minByPricefloorOrNull(
            demandId = DemandId(demandId = "demand id123"),
            priceFloor = 2.9
        )
        assertThat(result).isEqualTo(
            null
        )
    }
}