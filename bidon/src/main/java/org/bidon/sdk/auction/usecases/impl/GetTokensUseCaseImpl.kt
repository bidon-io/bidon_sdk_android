package org.bidon.sdk.auction.usecases.impl

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.ext.applyRegulation
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.TokenInfo
import org.bidon.sdk.auction.usecases.GetTokensUseCase
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.ext.SystemTimeNow
import org.bidon.sdk.utils.ext.TAG

internal class GetTokensUseCaseImpl : GetTokensUseCase {

    override suspend fun invoke(
        adTypeParam: AdTypeParam,
        adaptersSource: AdaptersSource,
        tokenTimeout: Long,
    ): Map<String, TokenInfo> = withContext(SdkDispatchers.Default) {
        // Filter and apply regulations to adapters
        val biddingAdapters = adaptersSource.adapters
            .filterIsInstance<Adapter.Bidding>()
            .onEach(Adapter::applyRegulation)

        // Fetch tokens concurrently, handling failures with supervisorScope
        supervisorScope {
            biddingAdapters.map { adapter ->
                async { adapter.demandId.demandId to getTokenInfo(adapter, adTypeParam, tokenTimeout) }
            }.awaitAll().toMap()
        }.also { logTokens(it) }
    }

    private suspend fun getTokenInfo(
        adapter: Adapter.Bidding,
        adTypeParam: AdTypeParam,
        tokenTimeout: Long
    ): TokenInfo {
        val tokenStartTs = SystemTimeNow

        // Fetch token with a timeout
        val (token, status) = withTimeoutOrNull(tokenTimeout) {
            adapter.getToken(adTypeParam)?.let {
                it to TokenInfo.Status.SUCCESS
            } ?: (null to TokenInfo.Status.NO_TOKEN)
        } ?: (null to TokenInfo.Status.TIMEOUT_REACHED)

        val tokenFinishTs = SystemTimeNow

        return TokenInfo(
            token = token,
            tokenStartTs = tokenStartTs,
            tokenFinishTs = tokenFinishTs,
            status = status.code
        )
    }

    private fun logTokens(tokens: Map<String, TokenInfo>) {
        tokens.forEach { (key, tokenInfo) ->
            logInfo(TAG, "#$key: status: ${tokenInfo.status}, token: ${tokenInfo.token}")
        }
    }
}
