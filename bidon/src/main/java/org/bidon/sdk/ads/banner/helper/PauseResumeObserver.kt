package org.bidon.sdk.ads.banner.helper

import kotlinx.coroutines.flow.StateFlow
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface PauseResumeObserver {
    val lifecycleFlow: StateFlow<ActivityLifecycleState>
}
