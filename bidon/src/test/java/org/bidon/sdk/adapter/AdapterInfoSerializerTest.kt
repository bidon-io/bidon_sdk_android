package org.bidon.sdk.adapter

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 24/02/2023.
 */
internal class AdapterInfoSerializerTest {

    @Test
    fun `AdapterInfo Serializer`() {
        val actual = AdapterInfo(
            adapterVersion = "123",
            sdkVersion = "456"
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "version" hasValue "123"
                "sdk_version" hasValue "456"
            }
        )
    }
}