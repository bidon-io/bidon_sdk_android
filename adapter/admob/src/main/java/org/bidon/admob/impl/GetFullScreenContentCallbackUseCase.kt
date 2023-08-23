package org.bidon.admob.impl

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import org.bidon.admob.asBidonError
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
internal class GetFullScreenContentCallbackUseCase {
    fun createCallback(
        adEventFlow: AdEventFlow,
        getAd: () -> Ad
    ): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked: $this")
                adEventFlow.emitEvent(AdEvent.Clicked(getAd()))
            }

            override fun onAdDismissedFullScreenContent() {
                logInfo(TAG, "onAdDismissedFullScreenContent: $this")
                adEventFlow.emitEvent(AdEvent.Closed(getAd()))
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(TAG, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                adEventFlow.emitEvent(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInfo(TAG, "onAdShown: $this")
                adEventFlow.emitEvent(AdEvent.Shown(getAd()))
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }
}

private const val TAG = "GetFullScreenContentCallback"