package org.bidon.sdk.stats.models

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.stats.impl.asSuccessResultOrFail
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 03/03/2023.
 */
internal class ResultBodySerializerTest {

    @Test
    fun `it should serialize WINNER`() {
        val actual = DemandStat.Network(
            roundStatus = RoundStatus.Win,
            ecpm = 1.234,
            adUnitId = "id123",
            demandId = DemandId("admob"),
            fillStartTs = 0L,
            fillFinishTs = 1L,
            bidStartTs = 2L,
            bidFinishTs = 3L,
        ).asSuccessResultOrFail(100000, 100020).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "status" hasValue "SUCCESS"
                "winner_id" hasValue "admob"
                "ecpm" hasValue 1.234
                "ad_unit_id" hasValue "id123"
                "auction_start_ts" hasValue 100000
                "auction_finish_ts" hasValue 100020
            }
        )
    }

    @Test
    fun `it should serialize FAILURE`() {
        val actual = DemandStat.Network(
            roundStatus = RoundStatus.NoBid,
            ecpm = 1.234,
            adUnitId = "id123",
            demandId = DemandId("admob"),
            fillStartTs = 0L,
            fillFinishTs = 1L,
            bidStartTs = 2L,
            bidFinishTs = 3L
        ).asSuccessResultOrFail(100000, 100020).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "status" hasValue "FAIL"
                "auction_start_ts" hasValue 100000
                "auction_finish_ts" hasValue 100020
            }
        )
    }
}