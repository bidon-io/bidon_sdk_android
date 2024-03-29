package org.bidon.gam.impl

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import org.bidon.gam.asBidonError
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

internal class GetFullScreenContentCallbackUseCase {
    fun createCallback(
        adEventFlow: AdEventFlow,
        getAd: () -> Ad?,
        onClosed: () -> Unit
    ): FullScreenContentCallback {
        return object : FullScreenContentCallback() {
            override fun onAdClicked() {
                logInfo(TAG, "onAdClicked: $this")
                getAd()?.let { adEventFlow.emitEvent(AdEvent.Clicked(it)) }
            }

            override fun onAdDismissedFullScreenContent() {
                logInfo(TAG, "onAdDismissedFullScreenContent: $this")
                getAd()?.let { adEventFlow.emitEvent(AdEvent.Closed(it)) }
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                logError(TAG, "onAdFailedToShowFullScreenContent: $this", error.asBidonError())
                adEventFlow.emitEvent(AdEvent.ShowFailed(error.asBidonError()))
            }

            override fun onAdImpression() {
                logInfo(TAG, "onAdShown: $this")
                getAd()?.let { adEventFlow.emitEvent(AdEvent.Shown(it)) }
            }

            override fun onAdShowedFullScreenContent() {}
        }
    }
}

private const val TAG = "GetFullScreenContentCallback"