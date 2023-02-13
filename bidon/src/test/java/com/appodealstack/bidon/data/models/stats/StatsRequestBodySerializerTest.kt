package com.appodealstack.bidon.data.models.stats

import com.appodealstack.bidon.stats.models.Demand
import com.appodealstack.bidon.stats.models.Round
import com.appodealstack.bidon.stats.models.StatsRequestBody
import com.appodealstack.bidon.utils.json.JsonParsers
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */
class StatsRequestBodySerializerTest {

    private val testee by lazy {
        JsonParsers
    }

    private val statRequestJsonStr = """
        {
          "auction_id": "id123",
          "auction_configuration_id": 4,
          "rounds": [
            {
              "winner_ecpm": 234.1,
              "winner_id": "asd",
              "id": "id43",
              "pricefloor": 34.2,
              "demands": [
                {
                  "ad_unit_id": "asd223",
                  "bid_finish_ts": 1,
                  "fill_start_ts": 4,
                  "ecpm": 1.2,
                  "fill_finish_ts": 3,
                  "id": "d345",
                  "bid_start_ts": 2,
                  "status": "code"
                },
                {
                  "id": "d6",
                  "status": "code2"
                }
              ]
            },
            {
              "id": "id43",
              "pricefloor": 34.2,
              "demands": []
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `it should serialize stat request`() {
        val json = testee.serialize(
            StatsRequestBody(
                auctionId = "id123",
                auctionConfigurationId = 4,
                rounds = listOf(
                    Round(
                        id = "id43",
                        demands = listOf(
                            Demand(
                                demandId = "d345",
                                adUnitId = "asd223",
                                ecpm = 1.2,
                                bidFinishTs = 1,
                                bidStartTs = 2,
                                fillFinishTs = 3,
                                fillStartTs = 4,
                                roundStatusCode = "code"
                            ),
                            Demand(
                                demandId = "d6",
                                roundStatusCode = "code2",
                                adUnitId = null,
                                ecpm = null,
                                bidFinishTs = null,
                                bidStartTs = null,
                                fillFinishTs = null,
                                fillStartTs = null,
                            )
                        ),
                        pricefloor = 34.2,
                        winnerDemandId = "asd",
                        winnerEcpm = 234.1
                    ),
                    Round(
                        id = "id43",
                        demands = listOf(),
                        pricefloor = 34.2,
                        winnerDemandId = null,
                        winnerEcpm = null
                    ),
                )
            )
        )
        assertThat(json.toString()).isEqualTo(JSONObject(statRequestJsonStr).toString())
    }
}