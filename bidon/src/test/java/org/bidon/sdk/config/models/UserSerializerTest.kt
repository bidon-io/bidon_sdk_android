package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 24/02/2023.
 */
internal class UserSerializerTest {

    @Test
    fun `User Serializer`() {
        val actual = User(
            platformAdvertisingId = "123",
            trackingAuthorizationStatus = "asd",
            applicationId = "a.a.a",
            consent = null,
            coppa = false
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "idfa" hasValue "123"
                "tracking_authorization_status" hasValue "asd"
                "idg" hasValue "a.a.a"
                "coppa" hasValue false
            }
        )
    }
}