package org.bidon.sdk.utils.ext

/**
 * Created by Bidon Team on 06/02/2023.
 */
fun <T : Any> T.asSuccess() = Result.success(this)
fun <T : Any> Throwable.asFailure() = Result.failure<T>(this)

inline fun <T> Result<T>.onAny(action: () -> Unit): Result<T> {
    action.invoke()
    return this
}

inline fun <T> Result<T>.mapFailure(action: (Throwable?) -> Throwable): Result<T> {
    return if (this.isFailure) {
        action.invoke(this.exceptionOrNull()).asFailure()
    } else {
        return this
    }
}
