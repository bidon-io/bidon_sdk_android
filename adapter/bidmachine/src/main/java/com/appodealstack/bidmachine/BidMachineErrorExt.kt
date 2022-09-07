package com.appodealstack.bidmachine

import com.appodealstack.bidon.domain.common.BidonError
import com.appodealstack.bidon.domain.common.DemandError
import com.appodealstack.bidon.domain.common.DemandId
import io.bidmachine.utils.BMError

internal fun BMError.asBidonError(demandId: DemandId): BidonError = when (this) {
    BMError.Request,
    BMError.Server,
    BMError.NoConnection -> DemandError.NetworkError(demandId)
    BMError.TimeoutError -> DemandError.NetworkTimeout(demandId)
    BMError.AlreadyShown -> DemandError.FullscreenAdAlreadyShowing(demandId)
    BMError.Expired -> DemandError.Expired(demandId)
    else -> {
        if (this.code == BMError.NO_CONTENT) {
            BidonError.NoFill(demandId)
        } else {
            DemandError.Unspecified(demandId)
        }
    }
}
