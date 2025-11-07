package org.bidon.sdk.stats.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.json.jsonArray
import org.bidon.sdk.utils.json.jsonObject
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject
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
            auctionConfigurationUid = "4",
            auctionPricefloor = 1.0,
            adUnits = listOf(
                StatsAdUnit(
                    demandId = "d345",
                    status = "WIN",
                    price = 1.2,
                    tokenStartTs = 2,
                    tokenFinishTs = 3,
                    bidType = BidType.CPM.code,
                    fillFinishTs = 3,
                    fillStartTs = 4,
                    adUnitUid = "123",
                    adUnitLabel = "label124",
                    ext = JSONObject(),
                    timeout = 5000
                ),
                StatsAdUnit(
                    demandId = "d6",
                    status = "NO_FILL",
                    price = null,
                    fillFinishTs = null,
                    fillStartTs = null,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    adUnitLabel = "label123",
                    adUnitUid = "123",
                    ext = JSONObject(),
                    timeout = 5000
                ),
                StatsAdUnit(
                    demandId = "d011",
                    price = 1.0,
                    status = "LOSE",
                    fillFinishTs = 6,
                    fillStartTs = 5,
                    bidType = BidType.RTB.code,
                    adUnitUid = "123",
                    adUnitLabel = "label123",
                    tokenStartTs = 678L,
                    tokenFinishTs = 679L,
                    ext = JSONObject(),
                    timeout = 5000
                )
            ),
            result = ResultBody(
                status = "SUCCESS",
                winnerDemandId = "d345",
                bidType = BidType.CPM.code,
                price = 1.2,
                winnerAdUnitUid = "123",
                winnerAdUnitLabel = "label124",
                auctionStartTs = 1000,
                auctionFinishTs = 1300,
                banner = null,
                rewarded = null,
                interstitial = null,
            ),
        ).serialize()
        println(json)
        json.assertEquals(
            expectedJsonStructure {
                "auction_id" hasValue "id123"
                "auction_configuration_id" hasValue 4
                "auction_configuration_uid" hasValue "4"
                "auction_pricefloor" hasValue 1.0
                "ad_units" hasArray jsonArray {
                    putValues(
                        listOf(
                            jsonObject {
                                "demand_id" hasValue "d345"
                                "status" hasValue "WIN"
                                "price" hasValue 1.2
                                "fill_start_ts" hasValue 4
                                "fill_finish_ts" hasValue 3
                                "bid_type" hasValue "CPM"
                                "ad_unit_uid" hasValue "123"
                                "ad_unit_label" hasValue "label124"
                                "token_start_ts" hasValue 2
                                "token_finish_ts" hasValue 3
                            },
                            jsonObject {
                                "demand_id" hasValue "d6"
                                "status" hasValue "NO_FILL"
                                "ad_unit_label" hasValue "label123"
                                "ad_unit_uid" hasValue "123"
                            },
                            jsonObject {
                                "demand_id" hasValue "d011"
                                "status" hasValue "LOSE"
                                "price" hasValue 1.0
                                "fill_start_ts" hasValue 5
                                "fill_finish_ts" hasValue 6
                                "bid_type" hasValue "RTB"
                                "ad_unit_uid" hasValue "123"
                                "ad_unit_label" hasValue "label123"
                                "token_start_ts" hasValue 678L
                                "token_finish_ts" hasValue 679L
                            }
                        )
                    )
                }
                "result" hasJson expectedJsonStructure {
                    "status" hasValue "SUCCESS"
                    "winner_demand_id" hasValue "d345"
                    "bid_type" hasValue "CPM"
                    "price" hasValue 1.2
                    "winner_ad_unit_uid" hasValue "123"
                    "winner_ad_unit_label" hasValue "label124"
                    "auction_start_ts" hasValue 1000
                    "auction_finish_ts" hasValue 1300
                }
            }
        )
    }

    @Test
    fun `test Bidding serialize`() {
        val actual = StatsAdUnit(
            demandId = "d011",
            status = "code3",
            price = 1.0,
            fillFinishTs = 6,
            fillStartTs = 5,
            bidType = BidType.RTB.code,
            adUnitLabel = "label123",
            adUnitUid = "123",
            tokenStartTs = 678L,
            tokenFinishTs = 679L,
            ext = JSONObject(),
            timeout = 5000
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "demand_id" hasValue "d011"
                "status" hasValue "code3"
                "price" hasValue 1.0
                "fill_start_ts" hasValue 5
                "fill_finish_ts" hasValue 6
                "bid_type" hasValue "RTB"
                "ad_unit_uid" hasValue "123"
                "ad_unit_label" hasValue "label123"
                "token_start_ts" hasValue 678L
                "token_finish_ts" hasValue 679L
            }
        )
    }
}
