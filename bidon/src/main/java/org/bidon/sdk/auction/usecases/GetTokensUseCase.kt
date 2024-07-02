package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.TokenInfo

internal interface GetTokensUseCase {
    suspend operator fun invoke(
        adType: AdType,
        adTypeParam: AdTypeParam,
        adaptersSource: AdaptersSource,
        tokenTimeout: Long,
    ): Map<String, TokenInfo>
}