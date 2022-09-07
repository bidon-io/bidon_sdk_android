package com.appodealstack.bidon.domain.common.impl

import com.appodealstack.bidon.domain.common.AutoRefresh
import com.appodealstack.bidon.domain.common.AutoRefresher
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.view.BannerAd
import com.appodealstack.bidon.view.DefaultAutoRefreshTimeoutMs
import com.appodealstack.bidon.view.helper.SdkDispatchers
import kotlinx.coroutines.*

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

    override fun launchRefreshIfNeeded() {
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
                    if (autoRefresh != AutoRefresh.Off) {
                        logInfo(Tag, "Auto-refresh timeout finish")
                        autoRefreshable.onRefresh()
                    }
                }
            }
        }
    }
}

private const val Tag = "AutoRefresher"
