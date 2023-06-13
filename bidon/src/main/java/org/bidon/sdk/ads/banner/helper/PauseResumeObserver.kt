package org.bidon.sdk.ads.banner.helper

import kotlinx.coroutines.flow.StateFlow
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal interface PauseResumeObserver {
    val lifecycleFlow: StateFlow<ActivityLifecycleState>
}
