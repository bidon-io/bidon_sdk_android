package org.bidon.sdk.auction.usecases.impl

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.TokenInfo
import org.bidon.sdk.auction.models.TokenResult
import org.bidon.sdk.auction.usecases.GetTokensUseCase
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.SystemTimeNow
import org.bidon.sdk.utils.ext.TAG

internal class GetTokensUseCaseImpl : GetTokensUseCase {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun invoke(
        adType: AdType,
        adTypeParam: AdTypeParam,
        adaptersSource: AdaptersSource,
        tokenTimeout: Long,
    ): Map<String, TokenInfo> {
        /**
         * Bidding demands auction
         */
        val filteredBiddingAdapters =
            adaptersSource.adapters.filterIsInstance<Adapter.Bidding>().onEach(::applyRegulation)

        /**
         * Tokens Obtaining
         */
        val tokens = filteredBiddingAdapters.getTokens(
            context = adTypeParam.activity.applicationContext,
            adTypeParam = adTypeParam,
            tokenTimeout = tokenTimeout
        ).mapNotNull { (key, value) -> value?.let { key to it } }
            .toMap()
            .onEach { pair ->
                logInfo(
                    TAG,
                    "#${pair.key}: status: ${pair.value.status} token:${pair.value.token}"
                )
            }

        return tokens
    }

    private fun applyRegulation(adapter: Adapter) {
        (adapter as? SupportsRegulation)?.let { supportsRegulation ->
            logInfo(
                TAG,
                "Applying regulation to ${adapter.demandId.demandId} <- " +
                    "GDPR=${BidonSdk.regulation.gdpr}, " +
                    "COPPA=${BidonSdk.regulation.coppa}, " +
                    "usPrivacyString=${BidonSdk.regulation.usPrivacyString}, " +
                    "gdprConsentString=${BidonSdk.regulation.gdprConsentString}"
            )
            supportsRegulation.updateRegulation(BidonSdk.regulation)
        }
    }

    private suspend fun List<Adapter.Bidding>.getTokens(
        context: Context,
        adTypeParam: AdTypeParam,
        tokenTimeout: Long,
    ): Map<String, TokenInfo?> =
        this.associate { adapter ->
            adapter.demandId.demandId to scope.async {
                val tokenStartTs = SystemTimeNow
                val tokenResult = withTimeoutOrNull(tokenTimeout) {
                    val token = adapter.getToken(
                        context = context,
                        adTypeParam = adTypeParam,
                    )
                    if (token.isNullOrEmpty()) {
                        TokenResult.NoToken
                    } else {
                        TokenResult.Success(token)
                    }
                } ?: TokenResult.TimeoutReached

                val tokenFinishTs = SystemTimeNow
                val (token, status) = when (tokenResult) {
                    is TokenResult.Success -> tokenResult.token to TokenInfo.Status.SUCCESS
                    TokenResult.NoToken -> null to TokenInfo.Status.NO_TOKEN
                    TokenResult.TimeoutReached -> null to TokenInfo.Status.TIMEOUT_REACHED
                }
                TokenInfo(
                    token = token,
                    tokenStartTs = tokenStartTs,
                    tokenFinishTs = tokenFinishTs,
                    status = status.code
                )
            }
        }.mapValues {
            it.value.await()
        }
}