package org.bidon.sdk.databinders.segment

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.config.models.json_scheme_utils.Whatever
import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.segment.SegmentSynchronizer
import org.bidon.sdk.segment.models.Gender
import org.bidon.sdk.segment.models.SegmentAttributes
import org.json.JSONObject
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 15/06/2023.
 */
internal class SegmentBinderTest {

    private val segmentSynchronizer: SegmentSynchronizer = mockk()
    private val testee by lazy {
        SegmentBinder(
            segmentSynchronizer = segmentSynchronizer
        )
    }

    @Test
    fun serialize() = runTest {
        every { segmentSynchronizer.segmentId } returns "0123456"
        every { segmentSynchronizer.attributes } returns SegmentAttributes(
            age = 28,
            gender = Gender.Female,
            customAttributes = mapOf(
                "k1" to "v1", "k2" to false
            ),
            inAppAmount = 100.0,
            isPaying = false,
            gameLevel = 58,
        )
        val segment = testee.getJsonObject()!!

        // check `id` is correct
        segment.assertEquals(
            expectedJsonStructure {
                "id" hasValue "0123456"

                // check `ext` JSON Encoded String later
                "ext" has Whatever.String
            }
        )

        // check 'ext' is correct
        val encodedString = segment.getString("ext")
        val encodedJson = JSONObject(encodedString)
        encodedJson.assertEquals(
            expectedJsonStructure {
                "gender" hasValue "FEMALE"
                "total_in_apps_amount" hasValue 100.0
                "is_paying" hasValue false
                "game_level" hasValue 58
                "age" hasValue 28
                "custom_attributes" hasValue mapOf(
                    "k1" to "v1",
                    "k2" to false
                )
            }
        )
    }
}