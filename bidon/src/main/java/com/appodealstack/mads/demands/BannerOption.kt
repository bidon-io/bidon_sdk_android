package com.appodealstack.mads.demands

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
}