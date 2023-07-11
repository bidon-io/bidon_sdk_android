package org.bidon.sdk.config.models

import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.utils.serializer.serialize
import org.junit.Test

/**
 * Created by Bidon Team on 23/02/2023.
 */
internal class GeoSerializerTest {

    @Test
    fun `geo serialize`() {
        val geo = Geo(
            lat = 52.2388276,
            lon = 20.9767103,
            accuracy = 13.407f,
            lastfix = 1677171891497,
            country = "Poland",
            city = "Warsaw",
            zip = "01-233",
            utcOffset = 1
        )
        val actual = geo.serialize()
        actual.assertEquals(
            expectedJsonStructure {
                "utcoffset" hasValue 1
                "accuracy" hasValue 13.407
                "lon" hasValue 20.9767103
                "lastfix" hasValue 1677171891497
                "lat" hasValue 52.2388276
                "country" hasValue "Poland"
                "city" hasValue "Warsaw"
                "zip" hasValue "01-233"
            }
        )
    }

    @Test
    fun `geo serialize with optional`() {
        val geo = Geo(
            lat = 52.2388276,
            lon = 20.9767103,
            accuracy = 13.407f,
            lastfix = 1677171891497,
            country = null,
            city = null,
            zip = null,
            utcOffset = 1
        )
        val actual = geo.serialize()
        actual.assertEquals(
            expectedJsonStructure {
                "utcoffset" hasValue 1
                "accuracy" hasValue 13.407
                "lon" hasValue 20.9767103
                "lastfix" hasValue 1677171891497
                "lat" hasValue 52.2388276
            }
        )
    }
}