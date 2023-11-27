package org.bidon.amazon.impl

import com.google.common.truth.Truth.assertThat
import org.bidon.amazon.SlotType
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.stats.models.BidType
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 */
internal class ParseSlotsUseCaseTest {

    private val testee by lazy { ParseSlotsUseCase() }

    @Test
    internal fun testInvoke() {
        val result = testee(
            listOf(
                AdUnit(
                    ext = "{\"slot_uuid\":\"slot_uuid_1\",\"format\":\"INTERSTITIAL\"}",
                    demandId = "amazon",
                    label = "amazon_bidding_mergeblock_ios_inter1",
                    pricefloor = null,
                    bidType = BidType.RTB,
                    uid = "1692462525087612452"
                ),
                AdUnit(
                    ext = "{\"slot_uuid\":\"slot_uuid_2\",\"format\":\"INTERSTITIAL\"}",
                    demandId = "amazon",
                    label = "amazon_bidding_mergeblock_ios_inter2",
                    pricefloor = null,
                    bidType = BidType.RTB,
                    uid = "2692462525087612452"
                ),
                AdUnit(
                    ext = "{\"slot_uuid\":\"slot_uuid_3\",\"format\":\"BANNER\"}",
                    demandId = "amazon",
                    label = "amazon_bidding_mergeblock_ios_inter3",
                    pricefloor = null,
                    bidType = BidType.RTB,
                    uid = "3692462525087612452"
                ),
                AdUnit(
                    ext = "{\"slot_uuid\":\"slot_uuid_10\",\"format\":\"MREC\"}",
                    demandId = "amazon",
                    label = "amazon_bidding_mergeblock_ios_inter10",
                    pricefloor = null,
                    bidType = BidType.RTB,
                    uid = "3692462525087612452"
                ),
            )
        )
        assertThat(result[SlotType.BANNER]).isEqualTo(listOf("slot_uuid_3"))
        assertThat(result[SlotType.INTERSTITIAL]).isEqualTo(listOf("slot_uuid_1", "slot_uuid_2"))
        assertThat(result[SlotType.MREC]).isEqualTo(listOf("slot_uuid_10"))
    }
}