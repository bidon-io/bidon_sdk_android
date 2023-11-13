package org.bidon.sdk.auction.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal class BidRequestBodyTest {
    @Test
    fun serialize() {
        val body = BidRequest(
            demands = mapOf(
                "bidmachine" to BidRequest.Token(token = "bm_token_123")
            ),
            banner = BannerRequest(formatCode = BannerRequest.StatFormat.AdaptiveBanner320x50.code),
            bidfloor = 1.24,
            orientationCode = AdObjectRequest.Orientation.Landscape.code,
            roundId = "round123",
            auctionId = "auc123",
            auctionConfigurationUid = "123",
            rewarded = RewardedRequest(),
            interstitial = InterstitialRequest(),
        )
        val actual = body.serialize()
        println(actual)

        actual.assertEquals(
            expectedJsonStructure {
                "demands" hasJson expectedJsonStructure {
                    "bidmachine" hasJson expectedJsonStructure {
                        "token" hasValue "bm_token_123"
                    }
                }

                "orientation" hasValue "LANDSCAPE"
                "rewarded" hasJson expectedJsonStructure { }
                "interstitial" hasJson expectedJsonStructure { }
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "ADAPTIVE"
                }
                "bidfloor" hasValue 1.24
                "round_id" hasValue "round123"
                "auction_id" hasValue "auc123"
                "auction_configuration_uid" hasValue "123"
            }
        )
    }

    @Test
    fun array() {
        val body = BidRequest(
            demands = mapOf(
                "bidmachine" to BidRequest.Token(token = "bm_token_123")
            ),
            banner = BannerRequest(formatCode = BannerRequest.StatFormat.AdaptiveBanner320x50.code),
            bidfloor = 1.24,
            orientationCode = AdObjectRequest.Orientation.Landscape.code,
            roundId = "round123",
            auctionId = "auc123",
            auctionConfigurationUid = "123",
            rewarded = RewardedRequest(),
            interstitial = InterstitialRequest(),
        )
        val actual = listOf(body).serialize()
        println(actual)
    }
}