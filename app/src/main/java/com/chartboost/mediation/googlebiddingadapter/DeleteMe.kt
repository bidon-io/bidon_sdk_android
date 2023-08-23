package com.chartboost.mediation.googlebiddingadapter

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by Aleksei Cherniaev on 21/08/2023.
 */
object DeleteMe {
    val payloadFlow = MutableStateFlow<AdType?>(null)

    fun setPayload(adType: AdType?) {
        payloadFlow.value = adType
    }

    sealed interface AdType {
        data class Interstitial(val payload: String) : AdType
        data class Rewarded(val payload: String) : AdType
        data class Banner(val payload: String) : AdType
    }
}