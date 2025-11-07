package org.bidon.sdk.config

import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.banner.BannerFormat

/**
 * Created by Bidon Team on 06/02/2023.
 */
public sealed class BidonError : Throwable() {

    public object SdkNotInitialized : BidonError()
    public object AppKeyIsInvalid : BidonError() {
        override val message: String = "App key is invalid"
    }

    public class InternalServerSdkError(override val message: String?) : BidonError()
    public class NetworkError(public val demandId: DemandId?, override val message: String? = null) : BidonError()

    /**
     * Only one auction per instance of an ad is possible
     */
    public object AuctionInProgress : BidonError()
    public object AuctionCancelled : BidonError()
    public object NoAuctionResults : BidonError()
    public object NoRoundResults : BidonError()

    public object NoContextFound : BidonError()
    public object NoBid : BidonError()
    public object AdNotReady : BidonError()
    public object NoAppropriateAdUnitId : BidonError()

    public class NoFill(public val demandId: DemandId) : BidonError()
    public class BidTimedOut(public val demandId: DemandId) : BidonError()
    public class FillTimedOut(public val demandId: DemandId) : BidonError()
    public class IncorrectAdUnit(public val demandId: DemandId, override val message: String) : BidonError()
    public class AdFormatIsNotSupported(public val demandId: String, public val bannerFormat: BannerFormat) : BidonError()
    public class Expired(public val demandId: DemandId?) : BidonError()

    public class Unspecified(
        public val demandId: DemandId?,
        override val cause: Throwable? = null,
        override val message: String = cause?.message ?: "NO_EXPLANATION_AVAILABLE"
    ) : BidonError()
}