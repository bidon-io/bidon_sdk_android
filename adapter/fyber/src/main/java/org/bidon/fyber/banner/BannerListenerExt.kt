package org.bidon.fyber.banner

import com.fyber.fairbid.ads.Banner
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.banner.BannerError
import com.fyber.fairbid.ads.banner.BannerListener
import kotlinx.coroutines.flow.MutableSharedFlow
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo

private const val TAG = "BannerListener"
internal fun MutableSharedFlow<BannerInterceptor>.initBannerListener() {
    val bannerInterceptorFlow = this
    Banner.setBannerListener(object : BannerListener {
        override fun onError(placementId: String, bannerError: BannerError?) {
            logError(TAG, "banner onError: $placementId", bannerError?.failure?.asBidonError())
            Banner.destroy(placementId)
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Error(placementId, bannerError?.failure.asBidonError())
            )
        }

        override fun onLoad(placementId: String) {
            logInfo(TAG, "banner onLoad: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Loaded(placementId)
            )
        }

        override fun onShow(placementId: String, impressionData: ImpressionData) {
            logInfo(TAG, "banner onShow: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Shown(placementId, impressionData)
            )
        }

        override fun onClick(placementId: String) {
            logInfo(TAG, "banner onClick: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.Clicked(placementId)
            )
        }

        override fun onRequestStart(placementId: String) {
            logInfo(TAG, "banner onRequestStart: $placementId")
            bannerInterceptorFlow.tryEmit(
                BannerInterceptor.RequestStarted(placementId)
            )
        }
    })
}
