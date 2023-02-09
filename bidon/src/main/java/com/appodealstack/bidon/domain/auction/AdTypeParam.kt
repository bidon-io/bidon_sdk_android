package com.appodealstack.bidon.domain.auction

import android.app.Activity
import android.view.ViewGroup
import com.appodealstack.bidon.domain.common.BannerSize
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal sealed interface AdTypeParam {
    val priceFloor: Double

    class Banner(val adContainer: ViewGroup, val bannerSize: BannerSize, override val priceFloor: Double) : AdTypeParam
    class Interstitial(val activity: Activity, override val priceFloor: Double) : AdTypeParam
    class Rewarded(val activity: Activity, override val priceFloor: Double) : AdTypeParam
}
