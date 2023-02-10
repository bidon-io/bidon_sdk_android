package com.appodealstack.bidon.config.domain.databinders

import android.util.Log
import com.appodealstack.bidon.base.ConcurrentTest
import com.appodealstack.bidon.data.binderdatasources.token.TokenDataSource
import com.appodealstack.bidon.data.json.jsonObject
import com.appodealstack.bidon.data.models.config.Token
import com.appodealstack.bidon.domain.databinders.TokenBinder
import com.appodealstack.bidon.domain.logging.impl.logError
import com.appodealstack.bidon.domain.logging.impl.logInfo
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TokenBinderTest : ConcurrentTest() {

    private val token = """"{\"some_key\":\"some_token_data\"}""""

    private val dataSource = mockk<TokenDataSource>()
    private val tokenBinder by lazy { TokenBinder(dataSource) }

    @Before
    fun before() {
        every { dataSource.getCachedToken() } returns Token(token)
        mockkStatic(Log::class)
        mockkStatic(::com.appodealstack.bidon.domain.logging.impl.logInfo)
        every { logInfo(any(), any()) } returns Unit
        every { logInfo(any(), any(), any()) } returns Unit
        every { logError(any(), any(), any()) } returns Unit
    }

    @Test
    fun `it must bind token to request body`() = runTest {
        val token = tokenBinder.getJsonObject()
        val json = jsonObject {
            tokenBinder.fieldName hasValue token
        }
        assertThat(json.toString()).isEqualTo("""{"token":"{\"some_key\":\"some_token_data\"}"}""")
    }
}