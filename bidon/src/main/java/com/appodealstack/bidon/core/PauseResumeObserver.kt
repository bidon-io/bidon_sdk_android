package com.appodealstack.bidon.core

import kotlinx.coroutines.flow.StateFlow

internal interface PauseResumeObserver {
    val lifecycleFlow: StateFlow<ActivityLifecycleState>
}
