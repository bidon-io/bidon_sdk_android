package org.bidon.sdk.auction.models

import com.google.common.truth.Truth.assertThat
import org.bidon.sdk.utils.json.jsonObject
import kotlin.test.Test

/**
 * Created by Aleksei Cherniaev on 25/10/2023.
 */
class BidResponseParserTest {

    @Test
    fun parse() {
        val json = jsonObject { }
        val actual = BidResponseParser().parseOrNull(json.toString())
        assertThat(actual).isEqualTo(
            BiddingResponse(
                bids = null,
                status = BiddingResponse.BidStatus.Success
            )
        )
        TODO()
    }
}