package com.appodealstack.mads.core

sealed interface InitializationResult {
    object Success : InitializationResult
    class Failed(val cause: Throwable) : InitializationResult
}