package com.appodealstack.bidon.config

import com.appodealstack.bidon.adapter.DemandId
import com.appodealstack.bidon.ads.banner.BannerSize

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
sealed class BidonError : Throwable() {

    object AppKeyIsInvalid : BidonError() {
        override val message: String = "App key is invalid"
    }

    class InternalServerSdkError(override val message: String?) : BidonError()
    class NetworkError(val demandId: DemandId?, override val message: String? = null) : BidonError()
    object NoAuctionResults : BidonError()
    object NoRoundResults : BidonError()

    object NoContextFound : BidonError()
    class NoBid(val demandId: DemandId) : BidonError()
    class NoFill(val demandId: DemandId) : BidonError()
    class BidTimedOut(val demandId: DemandId) : BidonError()
    class FillTimedOut(val demandId: DemandId) : BidonError()
    class AdFormatIsNotSupported(val demandId: String, val bannerSize: BannerSize) : BidonError()
    class Unspecified(val demandId: DemandId?, val sourceError: Throwable? = null) : BidonError()

    object FullscreenAdNotReady : BidonError()
    object NoAppropriateAdUnitId : BidonError()

    class Expired(val demandId: DemandId?) : BidonError()
}