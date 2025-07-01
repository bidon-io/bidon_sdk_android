package org.bidon.sdk.ads

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.config.SdkState
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.TAG
import java.util.concurrent.atomic.AtomicBoolean

internal interface InitAwaiter {
    suspend fun initWaitAndContinueIfRequired(
        onSuccess: suspend () -> Unit,
        onFailure: suspend () -> Unit
    )
}

internal class InitAwaiterImpl : InitAwaiter {

    private var isWaitingForInit = AtomicBoolean(true)

    override suspend fun initWaitAndContinueIfRequired(
        onSuccess: suspend () -> Unit,
        onFailure: suspend () -> Unit
    ) {
        when (BidonSdk.bidon.initializationState.value) {
            SdkState.Initialized -> onSuccess()
            SdkState.InitializationFailed -> {
                isWaitingForInit.set(true)
                onFailure()
            }
            else -> {
                logInfo(
                    TAG,
                    "Sdk is not initialized. Ad will load automatically when initialization was complete"
                )
                val result = BidonSdk.bidon.initializationState
                    .filter { it == SdkState.Initialized || it == SdkState.InitializationFailed }
                    .first()
                if (isWaitingForInit.compareAndSet(true, false)) {
                    when (result) {
                        SdkState.Initialized -> onSuccess()
                        SdkState.InitializationFailed -> onFailure()
                        else -> {}
                    }
                }
            }
        }
    }
}
