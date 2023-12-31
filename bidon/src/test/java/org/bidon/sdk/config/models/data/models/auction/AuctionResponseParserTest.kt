package org.bidon.sdk.config.models.data.models.auction

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResponseParser
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.utils.json.JsonParsers
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
            RoundRequest(
                id = "postbid",
                timeoutMs = 15,
                demandIds = listOf("admob", "bidmachine"),
                biddingIds = listOf(),
            ),
            RoundRequest(
                id = "prebid",
                timeoutMs = 25,
                demandIds = listOf("bidmachine"),
                biddingIds = listOf("asd"),
            ),
        ),
        auctionConfigurationId = 10,
        auctionId = "49975154-b82a-444b-a7f0-30bd749e7fce",
        token = "asdsad",
        lineItems = listOf(
            LineItem(
                demandId = "admob",
                pricefloor = 0.25,
                adUnitId = "AAAA2",
                uid = "1",
            ),
            LineItem(
                demandId = "bidmachine",
                pricefloor = 1.2235,
                adUnitId = "AAAA1",
                uid = "1",
            ),
        ),
        pricefloor = 0.01,
        externalWinNotificationsEnabled = false,
        auctionConfigurationUid = "10",
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
              ],
              "bidding": [
                "asd"
              ],
            }
          ],
          "line_items": [
            {
              "id": "admob",
              "pricefloor": 0.25,
              "ad_unit_id": "AAAA2",
               "uid": "1"
            },
           {
              "id": "bidmachine",
              "pricefloor": 1.2235,
              "ad_unit_id": "AAAA1",
               "uid": "1"
            }
          ],
          "token": "asdsad",
          "fill_timeout": 10000,
          "pricefloor": 0.01,
          "auction_id":"49975154-b82a-444b-a7f0-30bd749e7fce",
          "auction_configuration_id":10,
          "auction_configuration_uid":"10",
          "external_win_notifications":false
        }
    """.trimIndent()

    @Test
    fun `it should parse auction_configuration_id as String`() {
        val responseJsonStr = """
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
              ],
              "bidding": [
                "asd"
              ],
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
          "pricefloor": 0.01,
          "auction_id":"49975154-b82a-444b-a7f0-30bd749e7fce",
          "auction_configuration_id":"10",
          "auction_configuration_uid":"10923190123",
          "external_win_notifications":false
        }
        """.trimIndent()
        val res = AuctionResponseParser().parseOrNull(responseJsonStr)
        assertThat(res?.auctionConfigurationId).isEqualTo(10)
        assertThat(res?.auctionConfigurationUid).isEqualTo("10923190123")
    }
}