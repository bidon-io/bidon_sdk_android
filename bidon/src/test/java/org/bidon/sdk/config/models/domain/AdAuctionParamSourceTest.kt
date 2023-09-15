package org.bidon.sdk.config.models.domain

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.models.LineItem
import org.junit.Test

class AdAuctionParamSourceTest {
    @Test
    fun `it should find min LineItem with greater given pricefloor`() {
        val list = listOf(
            LineItem(
                demandId = "demand id123",
                pricefloor = 0.1,
                adUnitId = "unit 0.1",
                uid = "1",
            ),
            LineItem(
                demandId = null,
                pricefloor = 1.6,
                adUnitId = "unit 1.6",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 2.9,
                adUnitId = "unit 2.9",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.5,
                adUnitId = "unit 1.5",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.71,
                adUnitId = "unit 1.71",
                uid = "2",
            ),
            LineItem(
                demandId = "demand id321",
                pricefloor = 1.7,
                adUnitId = "unit 1.7",
                uid = "1",
            ),
        )
        val adAuctionParam = AdAuctionParamSource(
            activity = mockk(),
            pricefloor = 1.5,
            timeout = 1000,
            lineItems = list,
            onLineItemConsumed = {},
            json = null,
            optBannerFormat = null,
            optContainerWidth = null
        )
        val result = adAuctionParam.popLineItem(DemandId("demand id123"))

        assertThat(result).isEqualTo(
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.71,
                adUnitId = "unit 1.71",
                uid = "2",
            )
        )
    }

    @Test
    fun `it should NOT find any LineItem with greater given pricefloor`() {
        val list = listOf(
            LineItem(
                demandId = "demand id123",
                pricefloor = 0.1,
                adUnitId = "unit 0.1",
                uid = "1",
            ),
            LineItem(
                demandId = null,
                pricefloor = 1.6,
                adUnitId = "unit 1.6",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 2.9,
                adUnitId = "unit 2.9",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.5,
                adUnitId = "unit 1.5",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id123",
                pricefloor = 1.71,
                adUnitId = "unit 1.71",
                uid = "1",
            ),
            LineItem(
                demandId = "demand id321",
                pricefloor = 1.7,
                adUnitId = "unit 1.7",
                uid = "1",
            ),
        )
        val adAuctionParam = AdAuctionParamSource(
            activity = mockk(),
            pricefloor = 2.9,
            timeout = 1000,
            lineItems = list,
            onLineItemConsumed = {},
            json = null,
            optBannerFormat = null,
            optContainerWidth = null
        )
        val result = adAuctionParam.popLineItem(DemandId("demand id123"))
        assertThat(result).isEqualTo(
            null
        )
    }
}