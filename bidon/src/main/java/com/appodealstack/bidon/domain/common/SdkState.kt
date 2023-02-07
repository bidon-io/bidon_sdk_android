package com.appodealstack.bidon.domain.common

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal enum class SdkState {
    NotInitialized,
    Initializing,
    Initialized,
    InitializationFailed
}
