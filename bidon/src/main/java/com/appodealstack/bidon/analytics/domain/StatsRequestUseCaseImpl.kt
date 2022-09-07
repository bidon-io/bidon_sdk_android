package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.analytics.data.models.Demand
import com.appodealstack.bidon.analytics.data.models.Round
import com.appodealstack.bidon.analytics.data.models.StatsRequestBody
import com.appodealstack.bidon.auctions.data.models.RoundStat
import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.databinders.CreateRequestBodyUseCase
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.core.asUrlPathAdType
import com.appodealstack.bidon.core.errors.BaseResponse
import com.appodealstack.bidon.core.ext.logError
import com.appodealstack.bidon.core.ext.logInfo
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.utilities.ktor.JsonHttpRequest

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
            path = "$StatsRequestPath/${adType.asUrlPathAdType().lastSegment}",
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
                            startTs = demandStat.startTs,
                            finishTs = demandStat.finishTs
                        )
                    }
                )
            }
        )
    }
}

private const val StatsRequestPath = "stats"
private const val Tag = "StatsRequestUseCase"