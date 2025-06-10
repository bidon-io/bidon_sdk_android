package com.applovin.mediation.adapters.ext

import com.applovin.mediation.adapter.MaxAdapterError
import org.bidon.sdk.config.BidonError

internal fun BidonError.asMaxAdapterError() = when (this) {
    is BidonError.NoBid,
    is BidonError.NoFill,
    BidonError.NoRoundResults,
    BidonError.NoAuctionResults -> MaxAdapterError.NO_FILL

    is BidonError.FillTimedOut,
    is BidonError.BidTimedOut -> MaxAdapterError.TIMEOUT

    is BidonError.Expired -> MaxAdapterError.AD_EXPIRED
    is BidonError.InternalServerSdkError -> MaxAdapterError.SERVER_ERROR
    is BidonError.NetworkError -> MaxAdapterError.NO_CONNECTION

    is BidonError.AdFormatIsNotSupported,
    is BidonError.IncorrectAdUnit,
    BidonError.NoAppropriateAdUnitId,
    BidonError.AppKeyIsInvalid -> MaxAdapterError.INVALID_CONFIGURATION

    BidonError.AuctionInProgress,
    BidonError.AdNotReady -> MaxAdapterError.AD_NOT_READY

    is BidonError.Unspecified,
    BidonError.NoContextFound,
    BidonError.SdkNotInitialized -> MaxAdapterError.INTERNAL_ERROR

    BidonError.AuctionCancelled -> MaxAdapterError(
        MaxAdapterError.UNSPECIFIED.errorCode, // error code
        MaxAdapterError.UNSPECIFIED.errorMessage, // error message
        0, // adapter error code
        "Auction cancelled" // adapter error message
    )
}
