package org.bidon.sdk.auction.impl

import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.asSuccess

/**
 * Created by Bidon Team on 06/03/2023.
 */
internal class GetAuctionRequestUseCaseImpl : GetAuctionRequestUseCase {
    override suspend fun request(
        additionalData: AdTypeParam,
        auctionId: String,
        demandAd: DemandAd,
        adapters: Map<String, AdapterInfo>
    ): Result<AuctionResponse> {
        logInfo(TAG, "----------------------------- SERVERLESS DATA / USE ONLY FOR TEST ----------------------------- ")
        return ServerlessAuctionConfig.getAuctionResponse()!!.asSuccess()
    }
}

private const val TAG = "GetAuctionRequestUseCase"