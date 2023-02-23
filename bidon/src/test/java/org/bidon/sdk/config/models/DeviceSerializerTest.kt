package org.bidon.sdk.config.models

import com.appodealstack.bidon.json_scheme_utils.Whatever
import com.appodealstack.bidon.json_scheme_utils.assertEquals
import com.appodealstack.bidon.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 23/02/2023.
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
            javaScriptSupport = 1
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
            javaScriptSupport = null
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
            }
        )
    }
}