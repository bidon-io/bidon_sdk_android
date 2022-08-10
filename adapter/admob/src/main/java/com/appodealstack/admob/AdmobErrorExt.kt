package com.appodealstack.admob

import com.appodealstack.bidon.adapters.DemandError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError

internal fun LoadAdError.asBidonError(): DemandError = DemandError.Unspecified(AdmobDemandId)
internal fun AdError.asBidonError(): DemandError = DemandError.Unspecified(AdmobDemandId)