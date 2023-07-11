package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.Whatever
import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 23/02/2023.
 */
internal class DeviceSerializerTest {

    @Test
    fun `device serialization`() {
        val device = Device(
            userAgent = "",
            height = 123,
            width = 321,
            deviceModel = "model123",
            carrier = "carr2",
            connectionType = "WIFI",
            hardwareVersion = "123",
            language = "en",
            manufacturer = "sony",
            mccmnc = "mmm",
            os = "asd",
            osVersion = "djshkf",
            ppi = 123,
            pxRatio = 23.3f,
            javaScriptSupport = 1,
            type = "TABLET",
            osApiLevel = "33"
        )
        val actual = device.serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "connection_type" hasValue "WIFI"
                "os" hasValue "asd"
                "hwv" hasValue "123"
                "h" hasValue 123
                "ppi" hasValue 123
                "js" hasValue 1
                "language" hasValue "en"
                "ua" has Whatever.String
                "pxratio" has Whatever.Double
                "carrier" hasValue "carr2"
                "osv" hasValue "djshkf"
                "mccmnc" hasValue "mmm"
                "w" hasValue 321
                "model" hasValue "model123"
                "make" hasValue "sony"
                "type" hasValue "TABLET"
                "os_api_level" hasValue "33"
            }
        )
    }

    @Test
    fun `device serialization with optional value`() {
        val device = Device(
            userAgent = null,
            height = 123,
            width = null,
            deviceModel = "model123",
            carrier = null,
            connectionType = "WIFI",
            hardwareVersion = "123",
            language = null,
            manufacturer = "sony",
            mccmnc = null,
            os = "asd",
            osVersion = null,
            ppi = null,
            pxRatio = 23.3f,
            javaScriptSupport = null,
            type = "PHONE",
            osApiLevel = "33"
        )
        val actual = device.serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "connection_type" hasValue "WIFI"
                "os" hasValue "asd"
                "hwv" hasValue "123"
                "h" hasValue 123
                "pxratio" has Whatever.Double
                "model" hasValue "model123"
                "make" hasValue "sony"
                "type" hasValue "PHONE"
                "os_api_level" hasValue "33"
            }
        )
    }
}