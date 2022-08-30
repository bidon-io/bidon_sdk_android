package com.appodealstack.fyber.banner

import com.fyber.fairbid.ads.ImpressionData

sealed interface BannerInterceptor {
    class Error(val placementId: String, val cause: Throwable) : BannerInterceptor
    data class Loaded(val placementId: String) : BannerInterceptor
    class Shown(val placementId: String, val impressionData: ImpressionData) : BannerInterceptor
    data class Clicked(val placementId: String) : BannerInterceptor
    data class RequestStarted(val placementId: String) : BannerInterceptor
}
