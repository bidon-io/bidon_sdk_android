package com.appodealstack.mads.initializing

sealed interface InitializationResult {
    object Success : InitializationResult
    class Failed(val cause: Throwable) : InitializationResult
}