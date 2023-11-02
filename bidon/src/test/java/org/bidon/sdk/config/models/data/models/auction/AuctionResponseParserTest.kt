package org.bidon.sdk.config.models.data.models.auction

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResponseParser
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.JsonParsers
import org.bidon.sdk.utils.json.jsonObject
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
        auctionId = "49975154-b82a-444b-a7f0-30bd749e7fce",
        token = "asdsad",
        pricefloor = 0.01,
        externalWinNotificationsEnabled = false,
        auctionConfigurationUid = "10",
        adUnits = listOf(
            AdUnit(
                demandId = "admob",
                label = "admob_banner",
                pricefloor = 0.25,
                uid = "12387837129819",
                bidType = BidType.CPM,
                ext = jsonObject { "ad_unit_id" hasValue "ca-app-pub-3940256099942544/6300978111" }.toString(),
            ),
            AdUnit(
                demandId = "bidmachine",
                label = "bidmachine_banner",
                uid = "32387837129819",
                pricefloor = null,
                bidType = BidType.RTB,
                ext = null,
            )
        )
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
          "ad_units": [
            {
              "demand_id": "admob",
              "label": "admob_banner",
              "pricefloor": 0.25,
              "uid": "12387837129819",
              "bid_type": "CPM",
              "ext": {
                "ad_unit_id": "ca-app-pub-3940256099942544/6300978111"
              }
            },
            {
              "demand_id": "bidmachine",
              "label": "bidmachine_banner",
              "bid_type": "RTB",
              "uid": "32387837129819"
            }
          ],
          "token": "asdsad",
          "fill_timeout": 10000,
          "pricefloor": 0.01,
          "auction_id":"49975154-b82a-444b-a7f0-30bd749e7fce",
          "auction_configuration_uid":"10",
          "external_win_notifications":false
        }
    """.trimIndent()

    @Test
    fun `it should parse auction_configuration_uid as String`() {
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
          "token": "asdsad",
          "fill_timeout": 10000,
          "pricefloor": 0.01,
          "auction_id":"49975154-b82a-444b-a7f0-30bd749e7fce",
          "auction_configuration_uid":"10923190123",
          "external_win_notifications":false
        }
        """.trimIndent()
        val res = AuctionResponseParser().parseOrNull(responseJsonStr)
        assertThat(res?.auctionConfigurationUid).isEqualTo("10923190123")
    }
}