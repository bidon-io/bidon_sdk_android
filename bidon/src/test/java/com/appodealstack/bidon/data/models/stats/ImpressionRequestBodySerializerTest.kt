package org.bidon.sdk.data.models.stats

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.auction.models.InterstitialRequestBody
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.utils.json.JsonParsers
import org.json.JSONObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 08/02/2023.
 */
class ImpressionRequestBodySerializerTest {
    private val testee by lazy {
        JsonParsers
    }

    private val testJsonStr = """
        {
          "ad_unit_id": "adUnitId43",
          "demand_id": "demandId123",
          "ecpm": 2.33,
          "interstitial": {},
          "auction_id": "id123",
          "banner": {
            "format": "1"
          },
          "auction_configuration_id": 4,
          "imp_id": "impr123"
        }        
    """.trimIndent()

    @Test
    fun `it should serialize impression request`() {
        val json = testee.serialize(
            ImpressionRequestBody(
                auctionId = "id123",
                auctionConfigurationId = 4,
                impressionId = "impr123",
                ecpm = 2.33,
                demandId = "demandId123",
                rewarded = null,
                interstitial = InterstitialRequestBody(),
                banner = BannerRequestBody(formatCode = "1"),
                adUnitId = "adUnitId43"
            )
        )
        assertThat(json.toString()).isEqualTo(JSONObject(testJsonStr).toString())
    }
}