package org.bidon.admob.impl

import android.content.Context
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdFormat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.query.QueryInfo
import com.google.android.gms.ads.query.QueryInfoGenerationCallback
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.admob.AdmobInitParameters
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.auction.AdTypeParam
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
internal class GetTokenUseCase(private val configParams: AdmobInitParameters?) {
    suspend operator fun invoke(context: Context, adTypeParam: AdTypeParam): String? {
        val adRequest = AdRequest.Builder()
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
        val adFormat = adTypeParam.getAdFormat()
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

    private fun AdTypeParam.getAdFormat() =
        when (this) {
            is AdTypeParam.Banner -> AdFormat.BANNER
            is AdTypeParam.Interstitial -> AdFormat.INTERSTITIAL
            is AdTypeParam.Rewarded -> AdFormat.REWARDED
        }

    companion object {
        private const val DefaultTokenTimeoutMs = 1000L
    }
}