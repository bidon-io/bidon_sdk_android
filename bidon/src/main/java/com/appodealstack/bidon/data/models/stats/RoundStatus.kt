package com.appodealstack.bidon.data.models.stats

import com.appodealstack.bidon.domain.common.BidonError

/**
 * @see https://appodeal.atlassian.net/wiki/spaces/SX/pages/4490264831/SDK+Server+Schema#SDK%3C%3EServerSchema-StatsRequest
 */
enum class RoundStatus(val code: Int) {
    Win(1),
    Loss(2),
    NoBid(3),
    NoFill(4), // for Admob only NoBid possible
    UnknownAdapter(5),
    AdapterNotInitialized(6),
    BidTimeoutReached(7),
    FillTimeoutReached(8),
    NetworkError(9),
    IncorrectAdUnitId(10),
    NoAppropriateAdUnitId(11),
    AuctionCancelled(12),
    AdFormatNotSupported(13),
    UnspecifiedException(14),
    BelowPricefloor(15),

    Successful(-1), // Internal status
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
    null -> null
} ?: RoundStatus.UnspecifiedException