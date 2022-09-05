package com.appodealstack.bidon.analytics.data.models

import com.appodealstack.bidon.adapters.BidonError
import com.appodealstack.bidon.adapters.DemandError

enum class RoundStatus(val code: Int) {
    Successful(1),
    NoBid(2),
    NoFill(3), // for Admob only NoBid possible
    UnknownAdapter(4),
    AdapterNotInitialized(5),
    BidTimeoutReached(6),
    FillTimeoutReached(7),
    NetworkError(8),
    IncorrectAdUnitId(9),
    NoAppropriateAdUnitId(10),
    AuctionCancelled(11),
    AdFormatNotSupported(12),
    UnspecifiedException(13),
}

fun Throwable.asRoundStatus() = when (this as? BidonError) {
    is BidonError.AdFormatIsNotSupported -> RoundStatus.AdFormatNotSupported
    is BidonError.BidTimedOut -> RoundStatus.BidTimeoutReached
    is BidonError.FillTimedOut -> RoundStatus.FillTimeoutReached
    is BidonError.NetworkError -> RoundStatus.NetworkError
    BidonError.NoAppropriateAdUnitId -> RoundStatus.NoAppropriateAdUnitId
    is BidonError.NoFill -> RoundStatus.NoFill

    BidonError.FullscreenAdNotReady,
    BidonError.NoAuctionResults,
    BidonError.NoContextFound,
    BidonError.NoRoundResults,
    is BidonError.Unspecified,
    is DemandError.AdLoadFailed,
    is DemandError.BadCredential,
    is DemandError.BannerSizeNotSupported,
    is DemandError.Expired,
    is DemandError.FullscreenAdAlreadyShowing,
    is DemandError.FullscreenAdNotReady,
    is DemandError.NetworkError,
    is DemandError.NetworkTimeout,
    is DemandError.NoActivity,
    is DemandError.NoPlacement,
    is DemandError.Unspecified,
    null -> null
} ?: RoundStatus.UnspecifiedException