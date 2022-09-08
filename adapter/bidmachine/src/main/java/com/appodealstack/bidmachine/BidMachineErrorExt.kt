package com.appodealstack.bidmachine

import com.appodealstack.bidon.domain.common.BidonError
import com.appodealstack.bidon.domain.common.DemandId
import io.bidmachine.utils.BMError

internal fun BMError.asBidonError(demandId: DemandId): BidonError = when (this) {
    BMError.Request,
    BMError.Server,
    BMError.NoConnection -> BidonError.NetworkError(demandId)
    BMError.TimeoutError -> BidonError.NetworkError(demandId)
    BMError.AlreadyShown -> BidonError.FullscreenAdNotReady
    BMError.Expired -> BidonError.Expired(demandId)
    else -> {
        if (this.code == BMError.NO_CONTENT) {
            BidonError.NoFill(demandId)
        } else {
            BidonError.Unspecified(demandId)
        }
    }
}
