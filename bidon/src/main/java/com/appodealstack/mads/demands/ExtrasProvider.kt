package com.appodealstack.mads.demands

import android.os.Bundle

interface ExtrasProvider {
    fun setExtras(adParams: Bundle)
}

interface ExtrasSource {
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
}