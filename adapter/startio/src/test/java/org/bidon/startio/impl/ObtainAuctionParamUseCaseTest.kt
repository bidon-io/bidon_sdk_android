package org.bidon.startio.impl

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.junit.Test

class ObtainAuctionParamUseCaseTest {

    private val testee = ObtainAuctionParamUseCase()

    @Test
    fun `getFullscreenParam should return result`() {
        val auctionParamsScope = mockk<AdAuctionParamSource>(relaxed = true)

        val result = testee.getFullscreenParam(auctionParamsScope)

        assertThat(result).isNotNull()
    }

    @Test
    fun `getBannerParam should return result`() {
        val auctionParamsScope = mockk<AdAuctionParamSource>(relaxed = true)

        val result = testee.getBannerParam(auctionParamsScope)

        assertThat(result).isNotNull()
    }

    @Test
    fun `use case should be instantiable`() {
        assertThat(testee).isNotNull()
    }
}
