package com.appodealstack.ironsource

import com.appodealstack.mads.demands.AdapterParameters
import com.ironsource.mediationsdk.IronSource

data class IronSourceParameters(val appKey: String, val adUnit: IronSource.AD_UNIT? = null) : AdapterParameters