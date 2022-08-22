package com.appodealstack.bidmachine.ext

import com.appodealstack.bidmachine.BidMachineParameters
import com.appodealstack.bidon.core.parse
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test

class ParserTest {

    @Test
    fun `it should parse ConfigRequest to BidMachineParameters`() {
        val jsonString =
            """{"admob":{},"bidmachine":{"seller_id":"1","endpoint":"https://x.bidmachine.com","mediation_config":["facebook"]},"applovin":{"applovin_key":"String"},"appsflyer":{"dev_key":"String","app_id":"String"}}""".trimIndent()
        val json = Json.decodeFromString<JsonObject>(jsonString)
        println(json)
        val result = json["bidmachine"]!!.parse(BidMachineParameters.serializer())

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
        val json = Json.decodeFromString<JsonObject>(jsonString)
        println(json)
        val result = json["bidmachine"]!!.parse(BidMachineParameters.serializer())

        assertThat(result).isEqualTo(
            BidMachineParameters(
                sellerId = "1",
                endpoint = null,
                mediationConfig = null
            )
        )
    }
}
