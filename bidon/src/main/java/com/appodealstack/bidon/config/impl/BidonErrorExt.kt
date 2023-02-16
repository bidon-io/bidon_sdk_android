package com.appodealstack.bidon.ads

import com.appodealstack.bidon.config.BidonError

/**
 * Created by Aleksei Cherniaev on 16/02/2023.
 */
internal fun Throwable.asUnspecified(): BidonError {
    return (this as? BidonError) ?: BidonError.Unspecified(
        demandId = null,
        sourceError = this
    )
}