package org.bidon.mobilefuse.ext

import android.content.Context
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenProvider
import com.mobilefuse.sdk.internal.MobileFuseBiddingTokenRequest
import com.mobilefuse.sdk.internal.TokenGeneratorListener
import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences
import org.bidon.sdk.BidonSdk
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 06/07/2023.
 */
internal object GetMobileFuseTokenUseCase {
    suspend operator fun invoke(context: Context, isTestMode: Boolean): String? {
        // Create our token request // Create our token request
        val regulation = BidonSdk.regulation
        val tokenRequest = MobileFuseBiddingTokenRequest(
            privacyPreferences = MobileFusePrivacyPreferences.Builder()
                .setSubjectToCoppa(regulation.coppaApplies)
                .setSubjectToGdpr(regulation.gdprConsent)
                .setGppConsentString(regulation.gdprConsentString)
                .setUsPrivacyConsentString(regulation.usPrivacyString)
                .build(),
            isTestMode = isTestMode
        )
        return suspendCoroutine {
            // Generate a token - asynchronous:
            MobileFuseBiddingTokenProvider.getToken(
                tokenRequest,
                context,
                object : TokenGeneratorListener {
                    override fun onTokenGenerated(token: String) {
                        it.resume(token)
                    }

                    override fun onTokenGenerationFailed(error: String) {
                        it.resume(null)
                    }
                }
            )
        }
    }
}