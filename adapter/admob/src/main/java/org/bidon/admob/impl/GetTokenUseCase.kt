package org.bidon.admob.impl

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.query.QueryInfo
import com.google.android.gms.ads.query.QueryInfoGenerationCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.admob.AdmobInitParameters
import org.bidon.admob.ext.asBundle
import org.bidon.admob.ext.getAdFormat
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.auction.AdTypeParam
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal object GetTokenUseCase {
    suspend operator fun invoke(
        configParams: AdmobInitParameters,
        adTypeParam: AdTypeParam
    ): String? {
        val adRequest = AdRequest.Builder()
            .apply {
                val networkExtras = BidonSdk.regulation.asBundle().apply {
                    configParams.queryInfoType?.let {
                        putString("query_info_type", it)
                    }
                }
                configParams.requestAgent?.let { agent ->
                    setRequestAgent(agent)
                }
                addNetworkExtrasBundle(AdMobAdapter::class.java, networkExtras)
            }
            .build()
        val adFormat = adTypeParam.getAdFormat()
        return withTimeoutOrNull(DefaultTokenTimeoutMs) {
            suspendCancellableCoroutine { continuation ->
                QueryInfo.generate(
                    adTypeParam.activity.applicationContext,
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
}

private const val DefaultTokenTimeoutMs = 1000L
