package com.appodealstack.fyber.banner

import com.appodealstack.bidon.domain.stats.impl.logInternal
import com.fyber.fairbid.ads.Banner
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.banner.BannerError
import com.fyber.fairbid.ads.banner.BannerListener
import kotlinx.coroutines.flow.MutableSharedFlow

private const val Tag = "BannerListener"
internal fun MutableSharedFlow<BannerInterceptor>.initBannerListener() {
    val bannerInterceptorFlow = this
    Banner.setBannerListener(object : BannerListener {
        override fun onError(placementId: String, bannerError: BannerError?) {
            logInternal(Tag, "banner onError: $placementId", bannerError?.failure?.asBidonError())
            Banner.destroy(placementId)
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Error(placementId, bannerError?.failure.asBidonError())
            )
        }

        override fun onLoad(placementId: String) {
            logInternal(Tag, "banner onLoad: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Loaded(placementId)
            )
        }

        override fun onShow(placementId: String, impressionData: ImpressionData) {
            logInternal(Tag, "banner onShow: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Shown(placementId, impressionData)
            )
        }

        override fun onClick(placementId: String) {
            logInternal(Tag, "banner onClick: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Clicked(placementId)
            )
        }

        override fun onRequestStart(placementId: String) {
            logInternal(Tag, "banner onRequestStart: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.RequestStarted(placementId)
            )
        }
    })
}
