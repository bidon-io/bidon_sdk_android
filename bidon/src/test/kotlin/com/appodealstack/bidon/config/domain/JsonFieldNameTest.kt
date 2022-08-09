package com.appodealstack.bidon.config.domain

import com.appodealstack.bidon.config.domain.ext.getJsonFieldNamesWithValues
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class JsonFieldNameTest {

    @Test
    fun `annotation test`() {
        val testMePack = TestMePack(
            fieldString = "b2",
            fieldInt = 100500,
            fieldShouldNotBeTaken = "c3"
        )
        val map = testMePack.getJsonFieldNamesWithValues()

        assertThat(map).containsExactlyEntriesIn(
            mapOf("field_name_1" to "b2", "field_name_2" to 100500)
        )
        println(map)
    }
}

internal data class TestMePack(
    @JsonFieldName("field_name_1")
    val fieldString: String,
    @JsonFieldName("field_name_2")
    val fieldInt: Int,
    @SkipField
    val fieldShouldNotBeTaken: String,
)