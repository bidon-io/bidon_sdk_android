package org.bidon.sdk.utils.di

import org.bidon.sdk.auction.impl.GetAuctionRequestUseCaseImpl
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.config.impl.GetConfigRequestUseCaseImpl
import org.bidon.sdk.config.usecases.GetConfigRequestUseCase

/**
 * Created by Aleksei Cherniaev on 06/03/2023.
 */
object FlavoredDI {
    fun init() {
        module {
            factory<GetAuctionRequestUseCase> {
                GetAuctionRequestUseCaseImpl(
                    createRequestBody = get(),
                    getOrientation = get()
                )
            }
            factory<GetConfigRequestUseCase> {
                GetConfigRequestUseCaseImpl(
                    createRequestBody = get(),
                    segmentDataSource = get()
                )
            }
        }
    }
}