package com.appodealstack.bidon.config

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal enum class SdkState {
    NotInitialized,
    Initializing,
    Initialized,
    InitializationFailed
}