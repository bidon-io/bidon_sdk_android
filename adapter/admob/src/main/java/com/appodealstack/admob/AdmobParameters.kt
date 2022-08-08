package com.appodealstack.admob

import com.appodealstack.bidon.demands.AdapterParameters

class AdmobParameters(
    val interstitials: Map<Double, String>,
    val rewarded: Map<Double, String>,
    val banners: Map<Double, String>,
): AdapterParameters