package org.bidon.sdk.ads

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 16/02/2023.
 */
internal fun Throwable.asUnspecified(): BidonError {
    return (this as? BidonError) ?: BidonError.Unspecified(
        demandId = null,
        sourceError = this
    )
}