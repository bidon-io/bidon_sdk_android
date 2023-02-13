package com.appodealstack.bidon.data.models.auction

import com.appodealstack.bidon.auction.models.AuctionResponse
import com.appodealstack.bidon.auction.models.LineItem
import com.appodealstack.bidon.auction.models.Round
import com.appodealstack.bidon.utils.json.JsonParsers
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */
internal class AuctionResponseParserTest {

    private val testee by lazy { JsonParsers }

    @Test
    fun `it should parse auction response`() {
        val result = testee.parseOrNull<AuctionResponse>(responseJsonStr)

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(expectedModel)
    }

    private val expectedModel = AuctionResponse(
        rounds = listOf(
            Round(
                id = "postbid",
                timeoutMs = 15,
                demandIds = listOf("admob", "bidmachine")
            ),
            Round(
                id = "prebid",
                timeoutMs = 25,
                demandIds = listOf("bidmachine")
            ),
        ),
        auctionConfigurationId = 10,
        auctionId = "49975154-b82a-444b-a7f0-30bd749e7fce",
        token = "asdsad",
        lineItems = listOf(
            LineItem(
                demandId = "admob",
                priceFloor = 0.25,
                adUnitId = "AAAA2"
            ),
            LineItem(
                demandId = "bidmachine",
                priceFloor = 1.2235,
                adUnitId = "AAAA1"
            ),
        ),
        fillTimeout = 10000,
        minPrice = 0.01
    )

    private val responseJsonStr = """
        {
          "rounds": [
            {
              "id": "postbid",
              "timeout": 15,
              "demands": [
                "admob",
                "bidmachine"
              ]
            },
            {
              "id": "prebid",
              "timeout": 25,
              "demands": [
                "bidmachine"
              ]
            }
          ],
          "line_items": [
            {
              "id": "admob",
              "pricefloor": 0.25,
              "ad_unit_id": "AAAA2"
            },
           {
              "id": "bidmachine",
              "pricefloor": 1.2235,
              "ad_unit_id": "AAAA1"
            }
          ],
          "token": "asdsad",
          "fill_timeout": 10000,
          "min_price": 0.01,
          "auction_id":"49975154-b82a-444b-a7f0-30bd749e7fce",
          "auction_configuration_id":10
        }
    """.trimIndent()
}