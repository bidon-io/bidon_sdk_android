package com.appodealstack.bidon.auctions.domain

import com.appodealstack.bidon.adapters.banners.AutoRefresh
import com.appodealstack.bidon.core.SdkDispatchers
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.view.BannerAd
import com.appodealstack.bidon.view.DefaultAutoRefreshTimeoutMs
import kotlinx.coroutines.*

internal interface AutoRefresher {
    fun setAutoRefreshTimeout(timeoutMs: Long = DefaultAutoRefreshTimeoutMs)
    fun stopAutoRefresh()
    fun launchRefresh()
}

internal class AutoRefresherImpl(
    private val autoRefreshable: BannerAd.AutoRefreshable,
    private val dispatcher: CoroutineDispatcher = SdkDispatchers.Main
) : AutoRefresher {

    private val scope: CoroutineScope get() = CoroutineScope(dispatcher)
    private var autoRefreshJob: Job? = null
    private var autoRefresh: AutoRefresh = AutoRefresh.On(DefaultAutoRefreshTimeoutMs)

    override fun setAutoRefreshTimeout(timeoutMs: Long) {
        autoRefresh = if (timeoutMs == 0L) {
            AutoRefresh.Off
        } else {
            AutoRefresh.On(timeoutMs)
        }
    }

    override fun stopAutoRefresh() {
        autoRefresh = AutoRefresh.Off
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun launchRefresh() {
        proceedAutoRefresh()
    }

    private fun proceedAutoRefresh() {
        when (val ar = autoRefresh) {
            AutoRefresh.Off -> {
                // do nothing
            }
            is AutoRefresh.On -> {
                autoRefreshJob?.cancel()
                autoRefreshJob = scope.launch {
                    logInfo(Tag, "Auto-refresh timeout begin")
                    delay(ar.timeoutMs)
                    logInfo(Tag, "Auto-refresh timeout finish")
                    autoRefreshable.onRefresh()
                    autoRefreshJob = null
                }
            }
        }
    }
}

private const val Tag = "AutoRefresher"
