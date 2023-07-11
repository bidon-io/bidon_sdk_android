package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 24/02/2023.
 */
internal class AppSerializerTest {

    @Test
    fun `App serializer`() {
        val actual = App(
            bundle = "bndl",
            key = "asd",
            framework = "frm12",
            version = "123",
            frameworkVersion = "4546",
            pluginVersion = "97",
            bidonVersion = "bdn-0.0.1-beta"
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "bundle" hasValue "bndl"
                "key" hasValue "asd"
                "framework" hasValue "frm12"
                "version" hasValue "123"
                "framework_version" hasValue "4546"
                "plugin_version" hasValue "97"
                "bidon_version" hasValue "bdn-0.0.1-beta"
            }
        )
    }
    @Test
    fun `App serializer with optional`() {
        val actual = App(
            bundle = "bndl",
            key = null,
            framework = "frm12",
            version = null,
            frameworkVersion = null,
            pluginVersion = null,
            bidonVersion = "bdn-0.0.1-beta"
        ).serialize()

        actual.assertEquals(
            expectedJsonStructure {
                "bundle" hasValue "bndl"
                "framework" hasValue "frm12"
                "bidon_version" hasValue "bdn-0.0.1-beta"
            }
        )
    }
}