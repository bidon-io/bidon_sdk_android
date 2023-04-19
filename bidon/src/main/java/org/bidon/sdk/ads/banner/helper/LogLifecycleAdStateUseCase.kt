package org.bidon.sdk.ads.banner.helper

import org.bidon.sdk.logs.logging.impl.logInfo

/**
 * Created by Aleksei Cherniaev on 11/04/2023.
 */
internal object LogLifecycleAdStateUseCase {
    operator fun invoke(adLifecycle: AdLifecycle) {
        val message = when (adLifecycle) {
            AdLifecycle.Created,
            AdLifecycle.LoadingFailed,
            AdLifecycle.Loading -> "Banner not loaded"
            AdLifecycle.Displaying,
            AdLifecycle.Displayed -> "Banner shown"
            AdLifecycle.DisplayingFailed -> "Banner show failed"
            AdLifecycle.Destroyed -> "Banner destroyed"
            AdLifecycle.Loaded -> "Banner loaded"
        }
        logInfo("AdLifecycle", message)
    }
}