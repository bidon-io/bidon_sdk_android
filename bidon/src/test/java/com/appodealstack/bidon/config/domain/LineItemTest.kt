package org.bidon.sdk.config.domain

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.minByPricefloorOrNull
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LineItemTest {
    @Test
    fun `it should find min LineItem with greater given pricefloor`() {
        val list = listOf(
            LineItem(
                demandId = "demand id123",
                pricefloor = 0.1,
                adUnitId = "unit 0.1"
            ),
            LineItem(
                demandId = null,
                pricefloor = 1.6,
                adUnitId = "unit 1.6"
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 2.9,
                adUnitId = "unit 2.9"
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.5,
                adUnitId = "unit 1.5"
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.71,
                adUnitId = "unit 1.71"
            ),
            LineItem(
                demandId = "demand id321",
                pricefloor = 1.7,
                adUnitId = "unit 1.7"
            ),
        )
        val result = list.minByPricefloorOrNull(DemandId("demand id123"), 1.5)

        assertThat(result).isEqualTo(
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.71,
                adUnitId = "unit 1.71"
            )
        )
    }

    @Test
    fun `it should NOT find any LineItem with greater given pricefloor`() {
        val list = listOf(
            LineItem(
                demandId = "demand id123",
                pricefloor = 0.1,
                adUnitId = "unit 0.1"
            ),
            LineItem(
                demandId = null,
                pricefloor = 1.6,
                adUnitId = "unit 1.6"
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 2.9,
                adUnitId = "unit 2.9"
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.5,
                adUnitId = "unit 1.5"
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.71,
                adUnitId = "unit 1.71"
            ),
            LineItem(
                demandId = "demand id321",
                pricefloor = 1.7,
                adUnitId = "unit 1.7"
            ),
        )
        val result = list.minByPricefloorOrNull(
            demandId = DemandId(demandId = "demand id123"),
            pricefloor = 2.9
        )
        assertThat(result).isEqualTo(
            null
        )
    }
}