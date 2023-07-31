package org.bidon.sdk.auction.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 24/02/2023.
 */
internal class AdObjectRequestBodySerializerTest {

    @Test
    fun `AdObjectRequestBody Serializer full`() {
        val data = AdObjectRequest(
            pricefloor = 1.23,
            auctionId = "aId",
            orientationCode = AdObjectRequest.Orientation.Portrait.code,
            banner = BannerRequest(BannerRequest.StatFormat.LeaderBoard728x90.code),
            interstitial = InterstitialRequest(),
            rewarded = RewardedRequest(),
        )
        val actual = data.serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "pricefloor" hasValue 1.23
                "auction_id" hasValue "aId"
                "orientation" hasValue "PORTRAIT"
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "LEADERBOARD"
                }
                "interstitial" hasJson expectedJsonStructure { /* EMPTY */ }
                "rewarded" hasJson expectedJsonStructure { /* EMPTY */ }
            }
        )
    }

    @Test
    fun `AdObjectRequestBody Serializer only banner`() {
        val data = AdObjectRequest(
            pricefloor = 1.23,
            auctionId = "aId",
            orientationCode = AdObjectRequest.Orientation.Portrait.code,
            banner = BannerRequest(BannerRequest.StatFormat.LeaderBoard728x90.code),
            interstitial = null,
            rewarded = null,
        )
        val actual = data.serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "pricefloor" hasValue 1.23
                "auction_id" hasValue "aId"
                "orientation" hasValue "PORTRAIT"
                "banner" hasJson expectedJsonStructure {
                    "format" hasValue "LEADERBOARD"
                }
            }
        )
    }

    @Test
    fun `AdObjectRequestBody Serializer only INTERSTITIAL`() {
        val data = AdObjectRequest(
            pricefloor = 1.23,
            auctionId = "aId",
            orientationCode = AdObjectRequest.Orientation.Portrait.code,
            banner = null,
            interstitial = InterstitialRequest(),
            rewarded = null,
        )
        val actual = data.serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "pricefloor" hasValue 1.23
                "auction_id" hasValue "aId"
                "orientation" hasValue "PORTRAIT"
                "interstitial" hasJson expectedJsonStructure { /* EMPTY */ }
            }
        )
    }

    @Test
    fun `AdObjectRequestBody Serializer only REWARDED`() {
        val data = AdObjectRequest(
            pricefloor = 1.23,
            auctionId = "aId",
            orientationCode = AdObjectRequest.Orientation.Portrait.code,
            banner = null,
            interstitial = null,
            rewarded = RewardedRequest(),
        )
        val actual = data.serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "pricefloor" hasValue 1.23
                "auction_id" hasValue "aId"
                "orientation" hasValue "PORTRAIT"
                "rewarded" hasJson expectedJsonStructure { /* EMPTY */ }
            }
        )
    }
}