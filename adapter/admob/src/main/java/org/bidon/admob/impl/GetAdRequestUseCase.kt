package org.bidon.admob.impl

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk

internal class GetAdRequestUseCase {
    operator fun invoke() = AdRequest.Builder()
        .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
        .build()
}