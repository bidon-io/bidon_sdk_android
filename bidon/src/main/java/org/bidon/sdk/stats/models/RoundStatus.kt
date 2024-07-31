package org.bidon.sdk.stats.models

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed class RoundStatus(val code: String) {
    object Win : RoundStatus("WIN")
    object Lose : RoundStatus("LOSE")
    object NoBid : RoundStatus("NO_BID")
    object NoFill : RoundStatus("NO_FILL") // for Admob only NoBid possible
    object UnknownAdapter : RoundStatus("UNKNOWN_ADAPTER")
    object AdapterNotInitialized : RoundStatus("ADAPTER_NOT_INITIALIZED")
    object BidTimeoutReached : RoundStatus("BID_TIMEOUT_REACHED")
    object FillTimeoutReached : RoundStatus("FILL_TIMEOUT_REACHED")
    object NetworkError : RoundStatus("NETWORK_ERROR")
    class IncorrectAdUnit(val errorMessage: String?) : RoundStatus("INCORRECT_AD_UNIT")
    object NoAppropriateAdUnitId : RoundStatus("NO_APPROPRIATE_AD_UNIT_ID")
    object AuctionCancelled : RoundStatus("AUCTION_CANCELLED")
    object AdFormatNotSupported : RoundStatus("AD_FORMAT_NOT_SUPPORTED")
    class UnspecifiedException(val errorMessage: String?) : RoundStatus("UNSPECIFIED_EXCEPTION")
    object BelowPricefloor : RoundStatus("BELOW_PRICEFLOOR")

    object Successful : RoundStatus("INTERNAL_STATUS") // Internal status, its code should not be used
}

fun Throwable.asRoundStatus() = when (this as? BidonError) {
    is BidonError.AdFormatIsNotSupported -> RoundStatus.AdFormatNotSupported
    is BidonError.BidTimedOut -> RoundStatus.BidTimeoutReached
    is BidonError.FillTimedOut -> RoundStatus.FillTimeoutReached
    is BidonError.InternalServerSdkError,
    is BidonError.NetworkError -> RoundStatus.NetworkError
    BidonError.NoAppropriateAdUnitId -> RoundStatus.NoAppropriateAdUnitId
    is BidonError.NoFill -> RoundStatus.NoFill
    is BidonError.NoBid -> RoundStatus.NoBid
    BidonError.AuctionCancelled -> RoundStatus.AuctionCancelled

    is BidonError.AppKeyIsInvalid -> RoundStatus.UnspecifiedException("AppKeyIsInvalid")
    is BidonError.AdNotReady -> RoundStatus.UnspecifiedException("AdNotReady")
    is BidonError.NoAuctionResults -> RoundStatus.UnspecifiedException("NoAuctionResults")
    is BidonError.NoContextFound -> RoundStatus.UnspecifiedException("NoContextFound")
    is BidonError.NoRoundResults -> RoundStatus.UnspecifiedException("NoRoundResults")
    is BidonError.Expired -> RoundStatus.UnspecifiedException("Expired")
    is BidonError.Unspecified -> RoundStatus.UnspecifiedException((this as BidonError.Unspecified).sourceError?.message)
    is BidonError.IncorrectAdUnit -> RoundStatus.IncorrectAdUnit((this as BidonError.IncorrectAdUnit).message)
    is BidonError.AuctionInProgress -> RoundStatus.UnspecifiedException("AuctionInProgress")
    is BidonError.SdkNotInitialized -> RoundStatus.UnspecifiedException("SdkNotInitialized")
    null -> RoundStatus.UnspecifiedException(null)
}