package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 24/02/2023.
 */
internal class CappingSerializerTest {
    @Test
    fun `Capping Serializer`() {
        val actual = Capping(
            setting = "asd",
            value = 1
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "value" hasValue 1
                "setting" hasValue "asd"
            }
        )
    }
}