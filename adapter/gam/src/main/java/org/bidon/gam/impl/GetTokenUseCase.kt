package org.bidon.gam.impl

import android.content.Context
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.query.QueryInfo
import com.google.android.gms.ads.query.QueryInfoGenerationCallback
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.gam.GamInitParameters
import org.bidon.gam.ext.asBundle
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.AdType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class GetTokenUseCase(private val configParams: GamInitParameters?) {
    suspend operator fun invoke(context: Context, adType: AdType): String? {
        val adRequest = AdManagerAdRequest.Builder()
            .apply {
                val networkExtras = BidonSdk.regulation.asBundle().apply {
                    configParams?.queryInfoType?.let {
                        putString("query_info_type", it)
                    }
                }
                configParams?.requestAgent?.let { agent ->
                    setRequestAgent(agent)
                }
                addNetworkExtrasBundle(AdMobAdapter::class.java, networkExtras)
            }
            .build()
        val adFormat = when (adType) {
            AdType.Banner -> AdFormat.BANNER
            AdType.Interstitial -> AdFormat.INTERSTITIAL
            AdType.Rewarded -> AdFormat.REWARDED
        }
        return withTimeoutOrNull(DefaultTokenTimeoutMs) {
            suspendCoroutine { continuation ->
                QueryInfo.generate(
                    context,
                    adFormat,
                    adRequest,
                    object : QueryInfoGenerationCallback() {
                        override fun onSuccess(queryInfo: QueryInfo) {
                            continuation.resume(queryInfo.query)
                        }

                        override fun onFailure(errorMessage: String) {
                            continuation.resumeWithException(Exception(errorMessage))
                        }
                    }
                )
            }
        }
    }

    companion object {
        private const val DefaultTokenTimeoutMs = 1000L
    }
}