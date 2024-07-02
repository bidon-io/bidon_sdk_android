package org.bidon.sdk.config.models.data.models.stats

import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 08/02/2023.
 */
class ImpressionRequestBodySerializerTest {

    @Test
    fun `it should serialize impression request`() {
        val json = ImpressionRequestBody(
            auctionPricefloor = 0.01,
            auctionId = "id123",
            bidType = BidType.RTB.code,
            auctionConfigurationId = 4,
            auctionConfigurationUid = "4",
            demandId = "demandId123",
            adUnitUid = "1698961007059140608",
            adUnitLabel = "adUnitId43",
            price = 2.33,
            banner = BannerRequest(formatCode = "1"),
            interstitial = InterstitialRequest,
            rewarded = null
        ).serialize()

        json.assertEquals(
            expectedJsonStructure {
                "demand_id" hasValue "demandId123"
                "price" hasValue 2.33
                "interstitial" hasJson expectedJsonStructure {}
                "auction_id" hasValue "id123"
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "1"
                }
                "bid_type" hasValue "RTB"
                "auction_configuration_id" hasValue 4
                "auction_configuration_uid" hasValue 4UL
                "ad_unit_uid" hasValue "1698961007059140608"
                "ad_unit_label" hasValue "adUnitId43"
                "auction_pricefloor" hasValue 0.01
            }
        )
    }
}