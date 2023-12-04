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
            auctionId = "id123",
            impressionId = "impr123",
            price = 2.33,
            demandId = "demandId123",
            rewarded = null,
            interstitial = InterstitialRequest,
            banner = BannerRequest(formatCode = "1"),
            adUnitLabel = "adUnitId43",
            roundId = "round123",
            roundIndex = 2,
            bidType = BidType.RTB.code,
            auctionConfigurationUid = "4",
            adUnitUid = "1698961007059140608",
            roundPricefloor = 0.12,
            auctionPricefloor = 0.01
        ).serialize()

        json.assertEquals(
            expectedJsonStructure {
                "demand_id" hasValue "demandId123"
                "price" hasValue 2.33
                "interstitial" hasJson expectedJsonStructure {}
                "auction_id" hasValue "id123"
                "round_id" hasValue "round123"
                "round_idx" hasValue 2
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "1"
                }
                "bid_type" hasValue "RTB"
                "auction_configuration_uid" hasValue 4UL
                "imp_id" hasValue "impr123"
                "ad_unit_uid" hasValue "1698961007059140608"
                "ad_unit_label" hasValue "adUnitId43"
                "round_pricefloor" hasValue 0.12
                "auction_pricefloor" hasValue 0.01
            }
        )
    }
}