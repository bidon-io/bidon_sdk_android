package org.bidon.sdk.config.models.data.models.stats

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject
import org.junit.Test

/**
 * Created by Bidon Team on 08/02/2023.
 */
class ImpressionRequestBodySerializerTest {
    private val testJsonStr = """
        {
          "ad_unit_id": "adUnitId43",
          "demand_id": "demandId123",
          "ecpm": 2.33,
          "interstitial": {},
          "auction_id": "id123",
          "round_id": "round123",
          "round_idx": 2,
          "banner": {
            "format": "1"
          },
          "auction_configuration_id": 4,
          "imp_id": "impr123"
        }        
    """.trimIndent()

    @Test
    fun `it should serialize impression request`() {
        val json = ImpressionRequestBody(
            auctionId = "id123",
            auctionConfigurationId = 4,
            impressionId = "impr123",
            ecpm = 2.33,
            demandId = "demandId123",
            rewarded = null,
            interstitial = InterstitialRequest(),
            banner = BannerRequest(formatCode = "1"),
            adUnitId = "adUnitId43",
            roundId = "round123",
            roundIndex = 2,
        ).serialize()

        assertThat(json.toString()).isEqualTo(JSONObject(testJsonStr).toString())
    }
}