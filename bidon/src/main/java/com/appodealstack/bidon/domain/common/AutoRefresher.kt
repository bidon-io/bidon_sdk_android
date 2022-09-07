package com.appodealstack.bidon.domain.common

import com.appodealstack.bidon.view.DefaultAutoRefreshTimeoutMs

internal interface AutoRefresher {
    fun setAutoRefreshTimeout(timeoutMs: Long = DefaultAutoRefreshTimeoutMs)
    fun stopAutoRefresh()
    fun launchRefreshIfNeeded()
}