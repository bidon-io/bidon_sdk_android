package com.appodealstack.bidon.auction

import android.app.Activity
import android.view.ViewGroup
import com.appodealstack.bidon.ads.banner.BannerFormat
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal sealed interface AdTypeParam {
    val priceFloor: Double

    class Banner(val adContainer: ViewGroup, val bannerFormat: BannerFormat, override val priceFloor: Double) : AdTypeParam
    class Interstitial(val activity: Activity, override val priceFloor: Double) : AdTypeParam
    class Rewarded(val activity: Activity, override val priceFloor: Double) : AdTypeParam
}
