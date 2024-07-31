package org.bidon.bidmachine

import io.bidmachine.utils.BMError
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.config.BidonError

internal fun BMError.asBidonErrorOnFill(demandId: DemandId): BidonError = when (this) {
    BMError.Request,
    BMError.Server,
    BMError.NoConnection -> BidonError.NetworkError(demandId)
    BMError.TimeoutError -> BidonError.FillTimedOut(demandId)
    BMError.AlreadyShown -> BidonError.AdNotReady
    BMError.Expired -> BidonError.Expired(demandId)
    else -> {
        if (this.code == BMError.NO_CONTENT) {
            BidonError.NoFill(demandId)
        } else {
            BidonError.Unspecified(demandId, Throwable(message))
        }
    }
}

internal fun BMError.asBidonErrorOnBid(demandId: DemandId): BidonError = when (this) {
    BMError.Request,
    BMError.Server,
    BMError.NoConnection -> BidonError.NetworkError(demandId)
    BMError.TimeoutError -> BidonError.BidTimedOut(demandId)
    BMError.AlreadyShown -> BidonError.AdNotReady
    BMError.Expired -> BidonError.Expired(demandId)
    else -> {
        if (this.code == BMError.NO_CONTENT) {
            BidonError.NoBid
        } else {
            BidonError.Unspecified(demandId, Throwable(message))
        }
    }
}
