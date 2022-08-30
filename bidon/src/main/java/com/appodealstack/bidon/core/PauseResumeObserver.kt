package com.appodealstack.bidon.core

import kotlinx.coroutines.flow.StateFlow

internal interface PauseResumeObserver {
    val lifecycleFlow: StateFlow<LifecycleState>

    enum class LifecycleState {
        Created,
        Started,
        Resumed,
        Paused,
        Stopped,
        Destroyed,
    }
}
