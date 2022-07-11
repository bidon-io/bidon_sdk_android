package com.appodealstack.mads.demands.banners

/**
 * For banner ad views
 */
interface BannerAutoRefreshProvider {
    fun setAutoRefresh(autoRefresh: Boolean)
}

/**
 * For adapters
 */
interface BannerAutoRefreshSource {
    fun setAutoRefresh(autoRefresh: Boolean)
    fun isAutoRefresh(): Boolean
}

class BannerAutoRefreshSourceImpl : BannerAutoRefreshSource {
    private var autoRefresh: Boolean = true

    override fun setAutoRefresh(autoRefresh: Boolean) {
        this.autoRefresh = autoRefresh
    }

    override fun isAutoRefresh(): Boolean = autoRefresh
}