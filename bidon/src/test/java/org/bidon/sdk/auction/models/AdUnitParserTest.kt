package org.bidon.sdk.auction.models

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.stats.models.BidType
import org.json.JSONObject
import kotlin.test.Test

/**
 * Created by Aleksei Cherniaev on 24/10/2023.
 */
class AdUnitParserTest {

    @Test
    fun parse() {
        val json = JSONObject(
            """
            {
                "ad_units": [
                    {
                        "uid": "uid123",
                        "demand_id": "admob",
                        "pricefloor": 1.0,
                        "label": "label123",
                        "bid_type": "CPM",
                        "timeout": 5000,
                        "ext": {"a":1,"b":"c"}
                    },
                    {
                        "uid": "uid234",
                        "demand_id": "applovin",
                        "label": "label234",
                        "pricefloor": null,
                        "bid_type": "RTB",
                        "timeout": 5000,
                        "ext": {"b":1.44}
                    }
                ]
            }
            """.trimIndent()
        )

        val actual = buildList {
            repeat(json.getJSONArray("ad_units").length()) { index ->
                AdUnitParser()
                    .parseOrNull(
                        json.getJSONArray("ad_units")
                            .getJSONObject(index)
                            .toString()
                    )
                    ?.let {
                        add(it)
                    }
            }
        }
        println(actual[0])
        println(actual[1])

        assertThat(actual[0].extra?.getInt("a")).isEqualTo(1)
        assertThat(actual[0].extra?.getString("b")).isEqualTo("c")
        assertThat(actual[1].extra?.getDouble("b")).isEqualTo(1.44)

        assertThat(actual[0].uid).isEqualTo("uid123")
        assertThat(actual[0].demandId).isEqualTo("admob")
        assertThat(actual[0].pricefloor).isEqualTo(1.0)
        assertThat(actual[0].label).isEqualTo("label123")
        assertThat(actual[0].bidType).isEqualTo(BidType.CPM)
        assertThat(actual[0].extra.toString()).isEqualTo(JSONObject("""{"a":1,"b":"c"}""").toString())

        assertThat(actual[1].uid).isEqualTo("uid234")
        assertThat(actual[1].demandId).isEqualTo("applovin")
        assertThat(actual[1].pricefloor).isEqualTo(0.0)
        assertThat(actual[1].bidType).isEqualTo(BidType.RTB)
        assertThat(actual[1].label).isEqualTo("label234")
        assertThat(actual[1].extra.toString()).isEqualTo(JSONObject("""{"b":1.44}""").toString())
    }
}