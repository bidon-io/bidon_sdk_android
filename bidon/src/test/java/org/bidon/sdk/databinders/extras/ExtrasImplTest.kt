package org.bidon.sdk.databinders.extras

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 04/04/2023.
 */
internal class ExtrasImplTest {

    private val extras: Extras by lazy { ExtrasImpl() }

    class UnsupportedClass(
        val a: String
    )

    @Test
    fun `it should save only supported types`() {
        extras.addExtra("key_string", "string")
        extras.addExtra("key_int", 123)
        extras.addExtra("key_long", 12356L)
        extras.addExtra("key_float", 123.56f)
        extras.addExtra("key_double", 123567.56)
        extras.addExtra("key_boolean", true)
        extras.addExtra("key_char", 'D')
        extras.addExtra("key_unsupported", UnsupportedClass(a = "No way"))

        // THEN it should save only supported types
        assertThat(extras.getExtras()).containsExactlyEntriesIn(
            mapOf(
                "key_long" to 12356L,
                "key_int" to 123,
                "key_string" to "string",
                "key_char" to 'D',
                "key_double" to 123567.56,
                "key_float" to 123.56f,
                "key_boolean" to true,
            )
        )
        assertThat(extras.getExtras()).doesNotContainKey("key_unsupported")
    }

    @Test
    fun `it should remove existing data when null-value added`() {
        extras.addExtra("key_1", "ASD")
        extras.addExtra("key_2", "123")

        // CHECK all keys exist
        assertThat(extras.getExtras()).containsExactlyEntriesIn(
            mapOf(
                "key_1" to "ASD",
                "key_2" to "123",
            )
        )

        // WHEN null-value added
        extras.addExtra("key_1", null)

        // THEN it should remove existing data
        assertThat(extras.getExtras()).containsExactlyEntriesIn(
            mapOf(
                "key_2" to "123",
            )
        )
    }
}