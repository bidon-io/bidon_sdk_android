package org.bidon.sdk.config.domain.databinders

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.base.ConcurrentTest
import org.bidon.sdk.config.models.Token
import org.bidon.sdk.databinders.token.TokenBinder
import org.bidon.sdk.databinders.token.TokenDataSource
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.json.jsonObject
import org.junit.Before
import org.junit.Test

class TokenBinderTest : ConcurrentTest() {

    private val sourceResponseJson = "{\"name\":\"Error\",\"message\":\"hello\"}"

    private val dataSource = mockk<TokenDataSource>()
    private val tokenBinder by lazy { TokenBinder(dataSource) }

    @Before
    fun before() {
        every { dataSource.token } returns Token(sourceResponseJson)
        mockkStatic(Log::class)
        mockkStatic(::logInfo)
        every { logInfo(any(), any()) } returns Unit
        every { logInfo(any(), any()) } returns Unit
        every { logError(any(), any(), any()) } returns Unit
    }

    @Test
    fun `it must bind token to request body`() = runTest {
        val token = tokenBinder.getJsonObject()
        val json = jsonObject {
            tokenBinder.fieldName hasValue token
        }
        assertThat(json.toString()).isEqualTo("""{"token":"{\"name\":\"Error\",\"message\":\"hello\"}"}""".trimIndent())
    }
}