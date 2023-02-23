package org.bidon.fyber.banner

import android.app.Activity
import android.view.ViewGroup
import androidx.core.view.isVisible
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.ads.AdType
import org.bidon.fyber.banner.BNFyberBannerOption.Position
import com.fyber.fairbid.ads.Banner

object BNFyberBanner : FyberBanner by FyberBannerImpl()

interface FyberBanner {
    fun setBannerListener(fyberBannerListener: FyberBannerListener)

    // fun show(placementId: String, activity: Activity) - todo handle position.
    fun show(placementId: String, showOptions: BNFyberBannerOption, activity: Activity)
    fun destroy(placementId: String)
    fun getImpressionDepth(): Int

    fun startAutoRefresh(placementId: String)
    fun stopAutoRefresh(placementId: String)
    fun setAutoRefreshTimeout(placementId: String, timeoutMs: Long)
}

class FyberBannerImpl : FyberBanner {
    private val ads = mutableMapOf<String, DemandAd>()
    private val bannerViews = mutableMapOf<String, ViewGroup>()
    private val positions = mutableMapOf<String, Position>()
    private var fyberBannerListener: FyberBannerListener? = null

    override fun setBannerListener(fyberBannerListener: FyberBannerListener) {
        this.fyberBannerListener = fyberBannerListener
        ads.forEach { (placementId, demandAd) ->
            setBannerListener(demandAd, fyberBannerListener, placementId)
        }
    }

    override fun show(placementId: String, showOptions: BNFyberBannerOption, activity: Activity) {
        val position = showOptions.getPosition()
        require(position is Position.InViewGroup) {
            "Top/Bottom position is not implemented at the moment"
        }
        position.viewGroup.isVisible = false
        bannerViews[placementId] = position.viewGroup

        val adContainer = position.viewGroup

        val demandAd = getDemandAd(placementId)
        positions[placementId] = showOptions.getPosition()
        fyberBannerListener?.let {
            setBannerListener(
                demandAd = demandAd,
                fyberBannerListener = it,
                placementId = placementId
            )
        }
    }

    override fun destroy(placementId: String) {
        bannerViews[placementId]?.removeAllViews()
        bannerViews.remove(placementId)
    }

    override fun getImpressionDepth(): Int {
        return Banner.getImpressionDepth()
    }

    override fun startAutoRefresh(placementId: String) {
        val demandAd = getDemandAd(placementId)
    }

    override fun stopAutoRefresh(placementId: String) {
        val demandAd = getDemandAd(placementId)
    }

    override fun setAutoRefreshTimeout(placementId: String, timeoutMs: Long) {
        if (timeoutMs < 0) return
        val demandAd = getDemandAd(placementId)
    }

    private fun getDemandAd(placementId: String) = ads.getOrPut(placementId) {
        DemandAd(AdType.Banner)
    }

    private fun setBannerListener(
        demandAd: DemandAd,
        fyberBannerListener: FyberBannerListener,
        placementId: String
    ) {
    }
}
