package com.appodealstack.bidon.core

import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.auctions.data.models.AdTypeAdditional

internal enum class UrlPathAdType(val lastSegment: String) {
    Banner("banner"),
    Interstitial("interstitial"),
    Rewarded("rewarded_video"),
}

internal fun AdTypeAdditional.asUrlPathAdType(): UrlPathAdType = when (this) {
    is AdTypeAdditional.Banner -> UrlPathAdType.Banner
    is AdTypeAdditional.Interstitial -> UrlPathAdType.Interstitial
    is AdTypeAdditional.Rewarded -> UrlPathAdType.Rewarded
}

internal fun AdType.asUrlPathAdType(): UrlPathAdType = when (this) {
    AdType.Banner -> UrlPathAdType.Banner
    AdType.Interstitial -> UrlPathAdType.Interstitial
    AdType.Rewarded -> UrlPathAdType.Rewarded
}