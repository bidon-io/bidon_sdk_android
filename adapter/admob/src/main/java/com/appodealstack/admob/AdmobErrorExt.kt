package com.appodealstack.admob

import com.appodealstack.mads.demands.DemandError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

internal fun LoadAdError.asBidonError(): DemandError = when (this) {
    else -> DemandError.Unspecified
}

internal fun AdError.asBidonError(): DemandError = when (this) {
    else -> DemandError.Unspecified
}