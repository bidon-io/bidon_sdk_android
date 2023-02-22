package com.appodealstack.bidon.config.domain.databinders

import android.util.Log
import com.appodealstack.bidon.base.ConcurrentTest
import com.appodealstack.bidon.config.models.Token
import com.appodealstack.bidon.databinders.token.TokenBinder
import com.appodealstack.bidon.databinders.token.TokenDataSource
import com.appodealstack.bidon.logs.logging.impl.logError
import com.appodealstack.bidon.logs.logging.impl.logInfo
import com.appodealstack.bidon.utils.json.jsonObject
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
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