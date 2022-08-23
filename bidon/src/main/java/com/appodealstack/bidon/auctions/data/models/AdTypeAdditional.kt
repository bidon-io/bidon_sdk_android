package com.appodealstack.bidon.auctions.data.models

import android.app.Activity
import android.view.ViewGroup
import com.appodealstack.bidon.adapters.banners.BannerSize

internal sealed interface AdTypeAdditional {
    class Banner(val adContainer: ViewGroup, val bannerSize: BannerSize) : AdTypeAdditional
    class Interstitial(val activity: Activity) : AdTypeAdditional
    class Rewarded(val activity: Activity) : AdTypeAdditional
}
