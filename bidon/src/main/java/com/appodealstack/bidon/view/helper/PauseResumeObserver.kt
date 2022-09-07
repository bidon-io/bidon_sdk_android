package com.appodealstack.bidon.view.helper

import kotlinx.coroutines.flow.StateFlow

internal interface PauseResumeObserver {
    val lifecycleFlow: StateFlow<ActivityLifecycleState>
}
