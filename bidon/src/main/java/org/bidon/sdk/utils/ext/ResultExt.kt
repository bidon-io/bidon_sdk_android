package org.bidon.sdk.utils.ext

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal fun <T : Any> T.asSuccess(): Result<T> = Result.success(this)
internal fun <T : Any> Throwable.asFailure(): Result<T> = Result.failure<T>(this)

internal inline fun <T> Result<T>.onAny(action: () -> Unit): Result<T> {
    action.invoke()
    return this
}

internal inline fun <T> Result<T>.mapFailure(action: (Throwable?) -> Throwable): Result<T> {
    return if (this.isFailure) {
        action.invoke(this.exceptionOrNull()).asFailure()
    } else {
        return this
    }
}
