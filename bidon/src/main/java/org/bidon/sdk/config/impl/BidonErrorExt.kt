package org.bidon.sdk.config.impl

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 16/02/2023.
 */
internal fun Throwable.asBidonErrorOrUnspecified(): BidonError {
    return when {
        this is BidonError -> this
        isJobCancellationException(this) -> BidonError.AuctionCancelled
        else -> BidonError.Unspecified(
            demandId = null,
            sourceError = this
        )
    }
}

// TODO try to find more useful solution
private fun isJobCancellationException(throwable: Throwable): Boolean {
    return throwable::class.simpleName == "JobCancellationException"
}
