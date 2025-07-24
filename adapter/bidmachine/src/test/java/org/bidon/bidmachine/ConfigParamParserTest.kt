package org.bidon.bidmachine

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

internal class ConfigParamParserTest {

    @Test
    fun `it should parse ConfigRequest to BidMachineParameters`() {
        val jsonString =
            """{"admob":{},"bidmachine":{"seller_id":"1","endpoint":"https://x.bidmachine.com","mediation_config":["facebook"]},"applovin":{"applovin_key":"String"},"appsflyer":{"dev_key":"String","app_id":"String"}}""".trimIndent()
        val json = JSONObject(jsonString).getJSONObject("bidmachine")
        val bidMachineAdapter = BidMachineAdapter()
        val result = bidMachineAdapter.parseConfigParam(json.toString())
        assertThat(result).isEqualTo(
            BidMachineParameters(
                sellerId = "1",
                endpoint = "https://x.bidmachine.com",
                mediationConfig = listOf("facebook"),
                placements = null,
            )
        )
    }

    @Test
    fun `it should parse ConfigRequest to BidMachineParameters with absent params`() {
        val jsonString =
            """{"admob":{},"bidmachine":{"seller_id":"1"},"applovin":{"applovin_key":"String"},"appsflyer":{"dev_key":"String","app_id":"String"}}""".trimIndent()
        val json = JSONObject(jsonString).getJSONObject("bidmachine")
        val bidMachineAdapter = BidMachineAdapter()
        val result = bidMachineAdapter.parseConfigParam(json.toString())
        assertThat(result).isEqualTo(
            BidMachineParameters(
                sellerId = "1",
                endpoint = null,
                mediationConfig = null,
                placements = null,
            )
        )
    }

    @Test
    fun `it should parse ConfigRequest to BidMachineParameters with placements`() {
        val jsonString =
            """{"admob":{},"bidmachine":{"seller_id":"1","endpoint":"https://x.bidmachine.com","placements":{"banner":"banner_placement_id","interstitial":"interstitial_placement_id","rewarded":"rewarded_placement_id"}},"applovin":{"applovin_key":"String"},"appsflyer":{"dev_key":"String","app_id":"String"}}""".trimIndent()
        val json = JSONObject(jsonString).getJSONObject("bidmachine")
        val bidMachineAdapter = BidMachineAdapter()
        val result = bidMachineAdapter.parseConfigParam(json.toString())
        assertThat(result).isEqualTo(
            BidMachineParameters(
                sellerId = "1",
                endpoint = "https://x.bidmachine.com",
                mediationConfig = null,
                placements = mapOf(
                    "banner" to "banner_placement_id",
                    "interstitial" to "interstitial_placement_id",
                    "rewarded" to "rewarded_placement_id"
                ),
            )
        )
    }

    @Test
    fun `it should parse ConfigRequest to BidMachineParameters with empty placements`() {
        val jsonString =
            """{"admob":{},"bidmachine":{"seller_id":"1","placements":{}},"applovin":{"applovin_key":"String"},"appsflyer":{"dev_key":"String","app_id":"String"}}""".trimIndent()
        val json = JSONObject(jsonString).getJSONObject("bidmachine")
        val bidMachineAdapter = BidMachineAdapter()
        val result = bidMachineAdapter.parseConfigParam(json.toString())
        assertThat(result).isEqualTo(
            BidMachineParameters(
                sellerId = "1",
                endpoint = null,
                mediationConfig = null,
                placements = emptyMap(),
            )
        )
    }
}
