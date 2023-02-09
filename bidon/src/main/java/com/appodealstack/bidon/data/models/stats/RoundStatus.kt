package com.appodealstack.bidon.data.models.stats

import com.appodealstack.bidon.domain.common.BidonError

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * @see https://appodeal.atlassian.net/wiki/spaces/SX/pages/4490264831/SDK+Server+Schema#SDK%3C%3EServerSchema-StatsRequest
 */
enum class RoundStatus(val code: String) {
    Win("WIN"),
    Loss("LOSE"),
    NoBid("NO_BID"),
    NoFill("NO_FILL"), // for Admob only NoBid possible
    UnknownAdapter("UNKNOWN_ADAPTER"),
    AdapterNotInitialized("ADAPTER_NOT_INITIALIZED"),
    BidTimeoutReached("BID_TIMEOUT_REACHED"),
    FillTimeoutReached("FILL_TIMEOUT_REACHED"),
    NetworkError("NETWORK_ERROR"),
    IncorrectAdUnitId("INCORRECT_AD_UNIT"),
    NoAppropriateAdUnitId("NO_APPROPRIATE_AD_UNIT_ID"),
    AuctionCancelled("AUCTION_CANCELLED"),
    AdFormatNotSupported("AD_FORMAT_NOT_SUPPORTED"),
    UnspecifiedException("UNSPECIFIED_EXCEPTION"),
    BelowPricefloor("BELOW_PRICEFLOOR"),

    Successful("INTERNAL_STATUS"), // Internal status
}

fun Throwable.asRoundStatus() = when (this as? BidonError) {
    is BidonError.AdFormatIsNotSupported -> RoundStatus.AdFormatNotSupported
    is BidonError.BidTimedOut -> RoundStatus.BidTimeoutReached
    is BidonError.FillTimedOut -> RoundStatus.FillTimeoutReached
    is BidonError.NetworkError -> RoundStatus.NetworkError
    BidonError.NoAppropriateAdUnitId -> RoundStatus.NoAppropriateAdUnitId
    is BidonError.NoFill -> RoundStatus.NoFill
    is BidonError.NoBid -> RoundStatus.NoBid

    BidonError.FullscreenAdNotReady,
    BidonError.NoAuctionResults,
    BidonError.NoContextFound,
    BidonError.NoRoundResults,
    is BidonError.Expired,
    is BidonError.Unspecified,
    null,
    -> null
} ?: RoundStatus.UnspecifiedException