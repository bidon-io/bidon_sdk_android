package com.appodealstack.bidon.adapters

import android.os.Bundle

interface ExtrasProvider {
    fun setExtras(adParams: Bundle)
}

interface ExtrasSource {
    fun getExtras(demandAd: DemandAd): Bundle?
    fun setExtras(demandAd: DemandAd, adParams: Bundle)
}

class ExtrasSourceImpl : ExtrasSource {
    private val extras = mutableMapOf<DemandAd, Bundle>()

    override fun setExtras(demandAd: DemandAd, adParams: Bundle) {
        extras[demandAd] = adParams
    }

    override fun getExtras(demandAd: DemandAd): Bundle? {
        return extras[demandAd]
    }
}
