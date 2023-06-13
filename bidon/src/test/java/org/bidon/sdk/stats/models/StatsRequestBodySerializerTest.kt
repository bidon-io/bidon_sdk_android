package org.bidon.sdk.stats.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.json.jsonArray
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 24/02/2023.
 */
class StatsRequestBodySerializerTest {
    @Test
    fun `it should serialize stat request`() {
        val json = StatsRequestBody(
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
            ),
            result = ResultBody(
                status = "SUCCESS",
                demandId = "admob",
                ecpm = 0.123,
                adUnitId = "id123"
            )
        ).serialize()
        json.assertEquals(
            expectedJsonStructure {
                "auction_id" hasValue "id123"
                "auction_configuration_id" hasValue 4
                "rounds" hasArray jsonArray {
                    val list = listOf(
                        jsonObject {
                            "winner_ecpm" hasValue 234.1
                            "winner_id" hasValue "asd"
                            "id" hasValue "id43"
                            "pricefloor" hasValue 34.2

                            "demands" hasValue jsonArray {
                                putValues(
                                    listOf(
                                        jsonObject {
                                            "ad_unit_id" hasValue "asd223"
                                            "bid_finish_ts" hasValue 1
                                            "fill_start_ts" hasValue 4
                                            "ecpm" hasValue 1.2
                                            "fill_finish_ts" hasValue 3
                                            "id" hasValue "d345"
                                            "bid_start_ts" hasValue 2
                                            "status" hasValue "code"
                                        },
                                        jsonObject {
                                            "id" hasValue "d6"
                                            "status" hasValue "code2"
                                        }
                                    )
                                )
                            }
                        },
                        jsonObject {
                            "id" hasValue "id43"
                            "demands" hasValue jsonArray { }
                            "pricefloor" hasValue 34.2
                        }
                    )
                    putValues(list)
                }
                "result" hasJson expectedJsonStructure {
                    "status" hasValue "SUCCESS"
                    "winner_id" hasValue "admob"
                    "ecpm" hasValue 0.123
                    "ad_unit_id" hasValue "id123"
                }
            }
        )
    }
}
