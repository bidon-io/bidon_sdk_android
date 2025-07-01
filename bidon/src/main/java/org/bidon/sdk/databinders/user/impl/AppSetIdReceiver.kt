package org.bidon.sdk.databinders.user.impl

import android.content.Context
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AppSetIdReceiver(
    private val context: Context
) {
    private val cachedAppSetIdInfo = AtomicReference<AppSetIdInfo?>(null)
    private val mutex = Mutex()

    suspend fun getAppSetId() = getOrFetchAppSetIdInfo()?.id

    suspend fun getAppSetIdScope() = when (getOrFetchAppSetIdInfo()?.scope) {
        AppSetIdInfo.SCOPE_DEVELOPER -> DEVELOPER_SCOPE
        AppSetIdInfo.SCOPE_APP -> APP_SCOPE
        else -> null
    }

    private fun isDeveloperScope(scope: Int) = scope == AppSetIdInfo.SCOPE_DEVELOPER

    private suspend fun getOrFetchAppSetIdInfo(): AppSetIdInfo? {
        cachedAppSetIdInfo.get()?.let {
            logInfo(
                TAG,
                "Read AppSetId from cache. Id: ${it.id}, isDeveloperScope: ${isDeveloperScope(it.scope)}"
            )
            return it
        }
        return runCatching {
            mutex.withLock {
                logInfo(TAG, "Try to receive AppSetId")
                fetchAppSetIdInfo()?.also {
                    logInfo(TAG, "AppSetId received and kept to cache")
                    cachedAppSetIdInfo.set(it)
                }
            }
        }.onFailure { exception ->
            logError(TAG, "error during receiving AppSetId", exception)
        }.getOrNull()
    }

    private suspend fun fetchAppSetIdInfo(
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): AppSetIdInfo? = withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            try {
                AppSet.getClient(context).appSetIdInfo.addOnSuccessListener { info ->
                    if (continuation.isActive) {
                        logInfo(
                            TAG,
                            "AppSetId: Id: ${info.id}, isDeveloperScope: ${isDeveloperScope(info.scope)}"
                        )
                        continuation.resume(info)
                    }
                }.addOnFailureListener { e ->
                    if (continuation.isActive) {
                        logError(TAG, "AppSetId wasn't received. Exception", e)
                        continuation.resumeWithException(e)
                    }
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    logError(TAG, "AppSetId wasn't received. Exception", e)
                    continuation.resumeWithException(e)
                }
            }
        }
    }
}

private const val TAG = "AppSetIdInfoManager"
private const val DEFAULT_TIMEOUT_MS = 500L
private const val DEVELOPER_SCOPE = "developer"
private const val APP_SCOPE = "app"
