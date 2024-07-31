package org.bidon.sdk.auction.models

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject
import kotlin.test.Test

/**
 * Created by Aleksei Cherniaev on 25/10/2023.
 */
class BidResponseParserTest {

    @Test
    fun parse() {
        val json = JSONObject(
            """
{
  "bids": [
    {
      "ad_unit": {
        "demand_id": "vungle",
        "uid": "1633824270331281408",
        "label": "vungle_bidding_inter_mergeblock_ios",
        "bid_type": "RTB",
        "ext": {
          "placement_id": "INTER_TEST-5458572"
        }
      },
      "id": "652f976bfb91fd8aaa9c05ef",
      "imp_id": "10a81d26-c9f7-4a64-99cb-acc62a2a9053",
      "price": 2.32421875,
      "ext": {
        "payload": "some vungle payload"
      }
    },
    {
      "ad_unit": {
        "demand_id": "bidmachine",
        "bid_type": "CPM",
        "uid": "1633824270531921423",
        "label": "bidmachine_bidding_inter_mergeblock_ios"
      },
      "id": "aa225c05-80dd-408d-ac8d-36b98b7bf86a",
      "imp_id": "d3d846f9-001b-46e4-aed1-30f50c4e7e51",
      "price": 0.304724,
      "ext": {
        "payload": "some bidmachine payload"
      }
    }
  ],
  "status": "SUCCESS"
}
            """.trimIndent()
        )
        val actual = BidResponseParser().parseOrNull(json.toString())
        assertThat(actual).isEqualTo(
            BiddingResponse(
                bids = listOf(
                    BidResponse(
                        adUnit = AdUnit(
                            demandId = "vungle",
                            uid = "1633824270331281408",
                            label = "vungle_bidding_inter_mergeblock_ios",
                            ext = jsonObject {
                                "placement_id" hasValue "INTER_TEST-5458572"
                            }.toString(),
                            bidType = BidType.RTB,
                            pricefloor = null
                        ),
                        id = "652f976bfb91fd8aaa9c05ef",
                        impressionId = "10a81d26-c9f7-4a64-99cb-acc62a2a9053",
                        price = 2.32421875,
                        ext = jsonObject {
                            "payload" hasValue "some vungle payload"
                        }.toString()
                    ),
                    BidResponse(
                        adUnit = AdUnit(
                            demandId = "bidmachine",
                            uid = "1633824270531921423",
                            label = "bidmachine_bidding_inter_mergeblock_ios",
                            pricefloor = null,
                            bidType = BidType.CPM,
                            ext = null
                        ),
                        id = "aa225c05-80dd-408d-ac8d-36b98b7bf86a",
                        impressionId = "d3d846f9-001b-46e4-aed1-30f50c4e7e51",
                        price = 0.304724,
                        ext = jsonObject {
                            "payload" hasValue "some bidmachine payload"
                        }.toString()
                    )
                ),
                status = BiddingResponse.BidStatus.Success
            )
        )
    }
}