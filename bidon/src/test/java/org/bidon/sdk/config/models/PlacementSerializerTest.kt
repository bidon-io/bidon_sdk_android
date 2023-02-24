package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 24/02/2023.
 */
internal class PlacementSerializerTest {

    @Test
    fun `Placement Serializer`() {
        val actual = Placement(
            name = "na",
            reward = Reward(
                title = "PLN",
                amount = 12
            ),
            capping = Capping(
                setting = "a",
                value = 543
            )
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "name" hasValue "na"
                "reward" hasJson expectedJsonStructure {
                    "title" hasValue "PLN"
                    "value" hasValue 12
                }
                "capping" hasJson expectedJsonStructure {
                    "value" hasValue 543
                    "setting" hasValue "a"
                }
            }
        )
    }

    @Test
    fun `Placement Serializer with optional`() {
        val actual = Placement(
            name = "na",
            reward = null,
            capping = null
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "name" hasValue "na"
            }
        )
    }
}