package org.bidon.bidmachine.ext

import org.bidon.bidmachine.BidMachineAdapter
import org.bidon.bidmachine.BidMachineParameters
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test

class ParserTest {

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
                mediationConfig = listOf("facebook")
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
                mediationConfig = null
            )
        )
    }
}
