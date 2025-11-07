package org.bidon.sdk.stats.models

import org.bidon.sdk.config.BidonError

/**
 * Created by Bidon Team on 06/02/2023.
 */
public sealed class RoundStatus(public val code: String) {
    public object Win : RoundStatus("WIN")
    public object Lose : RoundStatus("LOSE")
    public object NoBid : RoundStatus("NO_BID")
    public object NoFill : RoundStatus("NO_FILL") // for Admob only NoBid possible
    public object UnknownAdapter : RoundStatus("UNKNOWN_ADAPTER")
    public object AdapterNotInitialized : RoundStatus("ADAPTER_NOT_INITIALIZED")
    public object BidTimeoutReached : RoundStatus("BID_TIMEOUT_REACHED")
    public object FillTimeoutReached : RoundStatus("FILL_TIMEOUT_REACHED")
    public object NetworkError : RoundStatus("NETWORK_ERROR")
    public class IncorrectAdUnit(public val errorMessage: String?) : RoundStatus("INCORRECT_AD_UNIT")
    public object NoAppropriateAdUnitId : RoundStatus("NO_APPROPRIATE_AD_UNIT_ID")
    public object AuctionCancelled : RoundStatus("AUCTION_CANCELLED")
    public object AdFormatNotSupported : RoundStatus("AD_FORMAT_NOT_SUPPORTED")
    public class UnspecifiedException(public val errorMessage: String?) : RoundStatus("UNSPECIFIED_EXCEPTION")
    public object BelowPricefloor : RoundStatus("BELOW_PRICEFLOOR")

    public object Successful : RoundStatus("INTERNAL_STATUS") // Internal status, its code should not be used
}

internal fun Throwable.asRoundStatus(): RoundStatus = when (this as? BidonError) {
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
    is BidonError.AuctionInProgress -> RoundStatus.UnspecifiedException("AuctionInProgress")
    is BidonError.SdkNotInitialized -> RoundStatus.UnspecifiedException("SdkNotInitialized")
    is BidonError.Unspecified -> RoundStatus.UnspecifiedException((this as BidonError.Unspecified).cause?.message)
    is BidonError.IncorrectAdUnit -> RoundStatus.IncorrectAdUnit((this as BidonError.IncorrectAdUnit).message)
    null -> RoundStatus.UnspecifiedException(message ?: "NO_EXPLANATION_AVAILABLE")
}