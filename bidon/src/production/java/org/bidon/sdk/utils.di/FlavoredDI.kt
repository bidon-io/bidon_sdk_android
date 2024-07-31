package org.bidon.sdk.utils.di

import org.bidon.sdk.auction.impl.GetAuctionRequestUseCaseImpl
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.config.impl.GetConfigRequestUseCaseImpl
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase

/**
 * Created by Bidon Team on 06/03/2023.
 */
object FlavoredDI {
    fun init() {
        module {
            factory<GetAuctionRequestUseCase> {
                GetAuctionRequestUseCaseImpl(
                    createRequestBody = get(),
                    getOrientation = get(),
                    segmentSynchronizer = get()
                )
            }
            factory<GetConfigRequestUseCase> {
                GetConfigRequestUseCaseImpl(
                    createRequestBody = get(),
                    segmentSynchronizer = get(),
                    biddingConfigSynchronizer = get()
                )
            }
        }
    }
}