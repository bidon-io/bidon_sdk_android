package com.appodealstack.bidon.data.networking.requests

import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.stats.Demand
import com.appodealstack.bidon.data.models.stats.Round
import com.appodealstack.bidon.data.models.stats.StatsRequestBody
import com.appodealstack.bidon.data.networking.BaseResponse
import com.appodealstack.bidon.data.networking.JsonHttpRequest
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.databinders.DataBinderType
import com.appodealstack.bidon.domain.stats.RoundStat
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.domain.stats.usecases.StatsRequestUseCase

internal class StatsRequestUseCaseImpl(
    private val createRequestBody: CreateRequestBodyUseCase
) : StatsRequestUseCase {
    private val binders: List<DataBinderType> = listOf(
        DataBinderType.Device,
        DataBinderType.App,
        DataBinderType.Token,
        DataBinderType.Geo,
        DataBinderType.Session,
        DataBinderType.User,
        DataBinderType.Segment,
    )

    override suspend operator fun invoke(
        auctionId: String,
        auctionConfigurationId: Int,
        results: List<RoundStat>,
        adType: AdType,
    ): Result<BaseResponse> = runCatching {
        val body = results.asStatsRequestBody(auctionId, auctionConfigurationId)
        val requestBody = createRequestBody(
            binders = binders,
            dataKeyName = "stats",
            data = body,
            dataSerializer = StatsRequestBody.serializer(),
        )
        logInfo("", "$requestBody")
        return get<JsonHttpRequest>().invoke(
            path = "$StatsRequestPath/${adType.code}",
            body = requestBody,
        ).map { jsonResponse ->
            BidonJson.decodeFromJsonElement(BaseResponse.serializer(), jsonResponse)
        }.onFailure {
            logError(Tag, "Error while sending stats", it)
        }.onSuccess {
            logInfo(Tag, "Stats was sent successfully")
        }
    }

    private fun List<RoundStat>.asStatsRequestBody(
        auctionId: String,
        auctionConfigurationId: Int,
    ): StatsRequestBody {
        return StatsRequestBody(
            auctionId = auctionId,
            auctionConfigurationId = auctionConfigurationId,
            rounds = this.map { stat ->
                Round(
                    id = stat.roundId,
                    winnerEcpm = stat.winnerEcpm,
                    winnerDemandId = stat.winnerDemandId?.demandId,
                    pricefloor = stat.priceFloor,
                    demands = stat.demands.map { demandStat ->
                        Demand(
                            demandId = demandStat.demandId.demandId,
                            adUnitId = demandStat.adUnitId,
                            roundStatusCode = demandStat.roundStatus.code,
                            ecpm = demandStat.ecpm,
                            bidStartTs = demandStat.bidStartTs,
                            bidFinishTs = demandStat.bidFinishTs,
                            fillStartTs = demandStat.fillStartTs,
                            fillFinishTs = demandStat.fillFinishTs,
                        )
                    }
                )
            }
        )
    }
}

private const val StatsRequestPath = "stats"
private const val Tag = "StatsRequestUseCase"