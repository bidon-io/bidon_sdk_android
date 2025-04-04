package org.bidon.sdk.config

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.banner.BannerFormat

/**
 * Created by Bidon Team on 06/02/2023.
 */
sealed class BidonError : Throwable() {

    object SdkNotInitialized : BidonError()
    object AppKeyIsInvalid : BidonError() {
        override val message: String = "App key is invalid"
    }

    class InternalServerSdkError(override val message: String?) : BidonError()
    class NetworkError(val demandId: DemandId?, override val message: String? = null) : BidonError()

    /**
     * Only one auction per instance of an ad is possible
     */
    object AuctionInProgress : BidonError()
    object AuctionCancelled : BidonError()
    object NoAuctionResults : BidonError()
    object NoRoundResults : BidonError()

    object NoContextFound : BidonError()
    object NoBid : BidonError()
    class NoFill(val demandId: DemandId) : BidonError()
    class BidTimedOut(val demandId: DemandId) : BidonError()
    class FillTimedOut(val demandId: DemandId) : BidonError()
    class IncorrectAdUnit(val demandId: DemandId, override val message: String) : BidonError()
    class AdFormatIsNotSupported(val demandId: String, val bannerFormat: BannerFormat) : BidonError()
    class Unspecified(val demandId: DemandId?, val sourceError: Throwable? = null) : BidonError()

    object AdNotReady : BidonError()
    object NoAppropriateAdUnitId : BidonError()

    class Expired(val demandId: DemandId?) : BidonError()
}