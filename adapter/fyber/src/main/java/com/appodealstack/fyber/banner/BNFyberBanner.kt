package com.appodealstack.fyber.banner

import android.app.Activity
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.appodealstack.fyber.PlacementKey
import com.appodealstack.fyber.banner.BNFyberBannerOption.Position
import com.appodealstack.bidon.core.DefaultAutoRefreshTimeoutMs
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.adapters.AdListener
import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.bidon.adapters.banners.AutoRefresh
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
    private var autoRefresh: AutoRefresh = AutoRefresh.On(timeoutMs = DefaultAutoRefreshTimeoutMs)

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
        if (autoRefresh == AutoRefresh.Off) {
            autoRefresh = AutoRefresh.On(DefaultAutoRefreshTimeoutMs)
        }
        val demandAd = getDemandAd(placementId)
    }

    override fun stopAutoRefresh(placementId: String) {
        val demandAd = getDemandAd(placementId)
        autoRefresh = AutoRefresh.Off
    }

    override fun setAutoRefreshTimeout(placementId: String, timeoutMs: Long) {
        if (timeoutMs < 0) return
        val demandAd = getDemandAd(placementId)
        autoRefresh = if (timeoutMs == 0L) AutoRefresh.Off else AutoRefresh.On(timeoutMs)
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
