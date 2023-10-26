package org.bidon.sdk.config.models.domain

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.auction.models.AdUnit
import org.junit.Test

class AdAuctionParamSourceTest {
    @Test
    fun `it should find min LineItem with greater given pricefloor`() {
        val list = listOf(
            AdUnit(
                uid = "1",
                demandId = "demand id123",
                pricefloor = 0.1,
                label = "unit 0.1",
                ext = null,
            ),
            AdUnit(
                uid = "2",
                demandId = "dem432",
                pricefloor = 1.6,
                label = "unit 1.6",
                ext = null,
            ),
            AdUnit(
                uid = "3",
                demandId = "demand id123",
                pricefloor = 2.9,
                label = "unit 2.9",
                ext = null,
            ),
            AdUnit(
                uid = "4",
                demandId = "demand id123",
                pricefloor = 1.5,
                label = "unit 1.5",
                ext = null,
            ),
            AdUnit(
                uid = "5",
                demandId = "demand id123",
                pricefloor = 1.71,
                label = "unit 1.71",
                ext = null,
            ),
            AdUnit(
                uid = "6",
                demandId = "demand id321",
                pricefloor = 1.7,
                label = "unit 1.7",
                ext = null,
            ),
        )
        val adAuctionParam = AdAuctionParamSource(
            activity = mockk(),
            pricefloor = 1.5,
            timeout = 1000,
            adUnits = list,
            onAdUnitsConsumed = {},
            json = null,
            optBannerFormat = null,
            optContainerWidth = null
        )
        val result = adAuctionParam.popAdUnit(DemandId("demand id123"))

        assertThat(result).isEqualTo(
            AdUnit(
                uid = "5",
                demandId = "demand id123",
                pricefloor = 1.71,
                label = "unit 1.71",
                ext = null,
            )
        )
    }

    @Test
    fun `it should NOT find any LineItem with greater given pricefloor`() {
        val list = listOf(
            AdUnit(
                uid = "1",
                demandId = "demand id123",
                pricefloor = 0.1,
                label = "unit 0.1",
                ext = null,
            ),
            AdUnit(
                uid = "2",
                demandId = "dem432",
                pricefloor = 1.6,
                label = "unit 1.6",
                ext = null,
            ),
            AdUnit(
                uid = "3",
                demandId = "demand id123",
                pricefloor = 2.9,
                label = "unit 2.9",
                ext = null,
            ),
            AdUnit(
                uid = "4",
                demandId = "demand id123",
                pricefloor = 1.5,
                label = "unit 1.5",
                ext = null,
            ),
            AdUnit(
                uid = "5",
                demandId = "demand id123",
                pricefloor = 1.71,
                label = "unit 1.71",
                ext = null,
            ),
            AdUnit(
                uid = "6",
                demandId = "demand id321",
                pricefloor = 1.7,
                label = "unit 1.7",
                ext = null,
            ),
        )
        val adAuctionParam = AdAuctionParamSource(
            activity = mockk(),
            pricefloor = 2.9,
            timeout = 1000,
            adUnits = list,
            onAdUnitsConsumed = {},
            json = null,
            optBannerFormat = null,
            optContainerWidth = null
        )
        val result = adAuctionParam.popAdUnit(DemandId("demand id123"))
        assertThat(result).isEqualTo(
            null
        )
    }
}