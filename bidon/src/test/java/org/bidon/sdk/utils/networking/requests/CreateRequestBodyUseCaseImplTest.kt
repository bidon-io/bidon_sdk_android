package org.bidon.sdk.utils.networking.requests

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.config.models.User
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.config.models.json_scheme_utils.assertEquals
import org.bidon.sdk.config.models.json_scheme_utils.expectedJsonStructure
import org.bidon.sdk.databinders.DataProvider
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.databinders.extras.ExtrasImpl
import org.junit.Test

/**
 * Created by Bidon Team on 04/04/2023.
 */
@ExperimentalCoroutinesApi
internal class CreateRequestBodyUseCaseImplTest : ConcurrentTest() {

    private val dataProvider: DataProvider = mockk()

    private val testee: CreateRequestBodyUseCase by lazy {
        CreateRequestBodyUseCaseImpl(dataProvider = dataProvider)
    }

    @Test
    fun `it should serialize data with key`() = runTest {
        val actual = testee.invoke(
            binders = emptyList(),
            extras = emptyMap(),
            data = User(
                platformAdvertisingId = "123",
                trackingAuthorizationStatus = "asd",
                applicationId = "a.a.a",
            ),
            dataKeyName = "user_key"
        )

        // THEN no ext should be
        assertThat(actual.has("ext")).isFalse()

        // THEN it should serialize data with key
        assertThat(actual.has("user_key")).isTrue()
        actual.getJSONObject("user_key").assertEquals(
            expectedJsonStructure {
                "idfa" hasValue "123"
                "tracking_authorization_status" hasValue "asd"
                "idg" hasValue "a.a.a"
            }
        )
    }

    @Test
    fun `it should serialize ext`() = runTest {
        // PREPARE extras
        val extras: Extras = ExtrasImpl()
        extras.addExtra("key1", 100500)
        extras.addExtra("key2", "asd")
        extras.addExtra("unsupported_object_type", mockk<Context>())

        val actual = testee.invoke(
            extras = extras.getExtras(),
            binders = emptyList(),
            data = User(
                platformAdvertisingId = "123",
                trackingAuthorizationStatus = "asd",
                applicationId = null,
            ),
            dataKeyName = "user_key"
        )

        // THEN it should serialize ext
        assertThat(actual.has("ext")).isTrue()
        println(actual)
        assertThat(actual.getString("ext")).isEqualTo(
            """
            {"key1":100500,"key2":"asd"}
            """.trimIndent()
        )
    }
}