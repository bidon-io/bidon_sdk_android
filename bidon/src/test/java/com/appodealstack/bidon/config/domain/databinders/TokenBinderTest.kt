package com.appodealstack.bidon.config.domain.databinders

import android.util.Log
import com.appodealstack.bidon.base.ConcurrentTest
import com.appodealstack.bidon.config.data.models.Token
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.core.ext.logInternal
import com.appodealstack.bidon.utilities.datasource.token.TokenDataSource
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
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
        mockkStatic(::logInfo)
        every { logInfo(any(), any()) } returns Unit
        every { logInternal(any(), any(), any()) } returns Unit
        every { logError(any(), any(), any()) } returns Unit
    }

    @Test
    fun `it must bind token to request body`() = runTest {
        val json = buildJsonObject {
            put(tokenBinder.fieldName, tokenBinder.getJsonElement())
        }
        assertThat(json.toString()).isEqualTo("""{"token":"{\"some_key\":\"some_token_data\"}"}""")
    }
}