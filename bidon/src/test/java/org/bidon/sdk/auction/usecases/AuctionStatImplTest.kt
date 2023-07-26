package org.bidon.sdk.auction.usecases

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.DeviceType
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.config.models.auctions.impl.Admob
import org.bidon.sdk.config.models.auctions.impl.Applovin
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.freezeTime
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.mockkLog
import org.bidon.sdk.stats.models.Demand
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.ext.asSuccess
import org.bidon.sdk.utils.networking.BaseResponse
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 27/06/2023.
 */
internal typealias SRound = org.bidon.sdk.stats.models.Round
internal typealias SBidding = org.bidon.sdk.stats.models.Bidding

@Ignore
@OptIn(ExperimentalCoroutinesApi::class)
internal class AuctionStatImplTest : ConcurrentTest() {

    private val statRequest: StatsRequestUseCase = mockk(relaxed = true)

    private val testee: AuctionStat by lazy {
        mockkObject(DeviceType)
        every { DeviceType.init(any()) } returns Unit
        mockkLog()
        freezeTime()
        AuctionStatImpl(statRequest)
    }

    @Before
    fun before() {
    }

    @After
    fun after() {
//        unmockkAll()
    }

    @Test
    fun `it should send AUCTION_CANCELLED state`() = runTest {
        coEvery {
            statRequest(
                demandAd = any(),
                statsRequestBody = any()
            )
        } returns BaseResponse(true, null).asSuccess()
        val auctionConfig = AuctionResponse(
            auctionConfigurationId = 10,
            auctionId = "auctionId_123",
            rounds = listOf(
                Round(
                    id = "round1",
                    timeoutMs = 1000L,
                    biddingIds = listOf("bi1", "bi2"),
                    demandIds = listOf("dem1", "dem2"),
                ),
                Round(
                    id = "round2",
                    timeoutMs = 25,
                    demandIds = listOf("dem3", "dem4"),
                    biddingIds = listOf("bi3"),
                ),
            ),
            lineItems = listOf(
                LineItem(
                    demandId = Applovin,
                    pricefloor = 0.25,
                    adUnitId = "AAAA2"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 1.2235,
                    adUnitId = "admob1"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 2.2235,
                    adUnitId = "admob2"
                ),
            ),
            pricefloor = 0.01,
            token = null,
            externalWinNotificationsEnabled = true
        )
        testee.markAuctionStarted(auctionId = "auctionId_123")
        testee.addRoundResults(
            round = auctionConfig.rounds?.first()!!,
            pricefloor = 1.4,
            roundResults = listOf(
                AuctionResult.Network.UnknownAdapter("dem1"),
                AuctionResult.Network.UnknownAdapter("dem2"),
                AuctionResult.Bidding.Failure.TimeoutReached,
            )
        )
        testee.markAuctionCanceled()
        testee.sendAuctionStats(
            auctionData = auctionConfig,
            demandAd = DemandAd(AdType.Interstitial)
        )
        coVerify(exactly = 1) {
            statRequest.invoke(
                demandAd = any(),
                statsRequestBody = StatsRequestBody(
                    auctionId = "auctionId_123",
                    auctionConfigurationId = 10,
                    result = ResultBody(
                        status = RoundStatus.AuctionCancelled.code,
                        demandId = null,
                        ecpm = null,
                        adUnitId = null,
                        auctionStartTs = 1000,
                        auctionFinishTs = 1000
                    ),
                    rounds = listOf(
                        SRound(
                            id = "round1",
                            pricefloor = 1.4,
                            winnerDemandId = null,
                            winnerEcpm = null,
                            demands = listOf(
                                asDemandStatNetwork("dem1", RoundStatus.UnknownAdapter),
                                asDemandStatNetwork("dem2", RoundStatus.UnknownAdapter),
                            ),
                            bidding = asDemandStatBidding(RoundStatus.BidTimeoutReached)
                        ),
                        SRound(
                            id = "round2",
                            pricefloor = 0.0,
                            winnerDemandId = null,
                            winnerEcpm = null,
                            demands = listOf(
                                asDemandStatNetwork("dem3", RoundStatus.AuctionCancelled),
                                asDemandStatNetwork("dem4", RoundStatus.AuctionCancelled)
                            ),
                            bidding = asDemandStatBidding(RoundStatus.AuctionCancelled)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `it should send AUCTION_CANCELLED state 2`() = runTest {
        coEvery {
            statRequest(
                demandAd = any(),
                statsRequestBody = any()
            )
        } returns BaseResponse(true, null).asSuccess()
        val auctionConfig = AuctionResponse(
            auctionConfigurationId = 10,
            auctionId = "auctionId_123",
            rounds = listOf(
                Round(
                    id = "round1",
                    timeoutMs = 1000L,
                    biddingIds = listOf("bi1", "bi2"),
                    demandIds = listOf("dem1", "dem2"),
                ),
                Round(
                    id = "round2",
                    timeoutMs = 25,
                    demandIds = listOf("dem3", "dem4"),
                    biddingIds = listOf("bi3"),
                ),
            ),
            lineItems = listOf(
                LineItem(
                    demandId = Applovin,
                    pricefloor = 0.25,
                    adUnitId = "AAAA2"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 1.2235,
                    adUnitId = "admob1"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 2.2235,
                    adUnitId = "admob2"
                ),
            ),
            pricefloor = 0.01,
            token = null,
            externalWinNotificationsEnabled = true
        )
        testee.markAuctionStarted(auctionId = "auctionId_123")
        testee.addRoundResults(
            round = auctionConfig.rounds?.first()!!,
            pricefloor = 1.4,
            roundResults = listOf(
                AuctionResult.Network.UnknownAdapter("dem1"),
                AuctionResult.Network.Success(
                    adSource = mockk(relaxed = true) {
                        val a = this
                        every { a.demandId } returns DemandId("dem2")
                        every { a.ad } returns Ad(
                            demandAd = DemandAd(AdType.Interstitial),
                            networkName = "dem2",
                            ecpm = 1.5,
                            adUnitId = null,
                            roundId = "r123",
                            currencyCode = AdValue.USD,
                            demandAdObject = mockk(relaxed = true),
                            dsp = null,
                            auctionId = "a123"
                        )
                    },
                    roundStatus = RoundStatus.Successful
                ),
                AuctionResult.Bidding.Failure.TimeoutReached,
            )
        )
        testee.markAuctionCanceled()
        testee.sendAuctionStats(
            auctionData = auctionConfig,
            demandAd = DemandAd(AdType.Interstitial)
        )
        val expect = StatsRequestBody(
            auctionId = "auctionId_123",
            auctionConfigurationId = 10,
            result = ResultBody(
                status = RoundStatus.AuctionCancelled.code,
                demandId = null,
                ecpm = null,
                adUnitId = null,
                auctionStartTs = 1000,
                auctionFinishTs = 1000
            ),
            rounds = listOf(
                SRound(
                    id = "round1",
                    pricefloor = 1.4,
                    winnerDemandId = "dem2",
                    winnerEcpm = 1.5,
                    demands = listOf(
                        asDemandStatNetwork("dem1", RoundStatus.UnknownAdapter),
                        Demand(
                            /**
                             * [RoundStatus.Win] should be mark as [RoundStatus.Loss]
                             */
                            roundStatusCode = RoundStatus.Loss.code,
                            demandId = "dem2",
                            bidStartTs = null,
                            bidFinishTs = null,
                            fillStartTs = 0,
                            fillFinishTs = 0,
                            adUnitId = "",
                            ecpm = 1.5,
                        )
                    ),
                    bidding = asDemandStatBidding(RoundStatus.BidTimeoutReached)
                ),
                SRound(
                    id = "round2",
                    pricefloor = 0.0,
                    winnerDemandId = null,
                    winnerEcpm = null,
                    demands = listOf(
                        asDemandStatNetwork("dem3", RoundStatus.AuctionCancelled),
                        asDemandStatNetwork("dem4", RoundStatus.AuctionCancelled)
                    ),
                    bidding = asDemandStatBidding(RoundStatus.AuctionCancelled)
                )
            )
        )
        coVerify(exactly = 1) {
            statRequest.invoke(
                demandAd = any(),
                statsRequestBody = eq(expect)
            )
        }
    }

    @Test
    fun `it should send WIN state`() = runTest {
        coEvery {
            statRequest(
                demandAd = any(),
                statsRequestBody = any()
            )
        } returns BaseResponse(true, null).asSuccess()
        val auctionConfig = AuctionResponse(
            auctionConfigurationId = 10,
            auctionId = "auctionId_123",
            rounds = listOf(
                Round(
                    id = "round1",
                    timeoutMs = 1000L,
                    biddingIds = listOf("bi1", "bi2"),
                    demandIds = listOf("dem1", "dem2"),
                ),
                Round(
                    id = "round2",
                    timeoutMs = 25,
                    demandIds = listOf("dem3", "dem4"),
                    biddingIds = listOf("bi3"),
                ),
            ),
            lineItems = listOf(
                LineItem(
                    demandId = Applovin,
                    pricefloor = 0.25,
                    adUnitId = "AAAA2"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 1.2235,
                    adUnitId = "admob1"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 2.2235,
                    adUnitId = "admob2"
                ),
            ),
            pricefloor = 0.01,
            token = null,
            externalWinNotificationsEnabled = true
        )
        testee.markAuctionStarted(auctionId = "auctionId_123")
        testee.addRoundResults(
            round = auctionConfig.rounds?.first()!!,
            pricefloor = 1.4,
            roundResults = listOf(
                AuctionResult.Network.UnknownAdapter("dem1"),
                AuctionResult.Network.UnknownAdapter("dem2"),
                AuctionResult.Bidding.Failure.TimeoutReached,
            )
        )
        testee.addRoundResults(
            round = auctionConfig.rounds[1],
            pricefloor = 1.5,
            roundResults = listOf(
                AuctionResult.Network.UnknownAdapter("dem3"),
                AuctionResult.Network.UnknownAdapter("dem4"),
                AuctionResult.Bidding.Success(
                    adSource = mockk(relaxed = true) {
                        val a = this
                        every { a.demandId } returns DemandId("bi3")
                        every { a.ad } returns Ad(
                            demandAd = DemandAd(AdType.Interstitial),
                            networkName = "admob",
                            ecpm = 1.5,
                            adUnitId = null,
                            roundId = "r123",
                            currencyCode = AdValue.USD,
                            demandAdObject = mockk(relaxed = true),
                            dsp = null,
                            auctionId = "a123"
                        )
                    },
                    roundStatus = RoundStatus.Successful
                ),
            )
        )
        testee.sendAuctionStats(
            auctionData = auctionConfig,
            demandAd = DemandAd(AdType.Interstitial)
        )
        val expect = StatsRequestBody(
            auctionId = "auctionId_123",
            auctionConfigurationId = 10,
            result = ResultBody(
                status = "SUCCESS",
                demandId = "bi3",
                ecpm = 1.5,
                adUnitId = null,
                auctionStartTs = 1000,
                auctionFinishTs = 1000
            ),
            rounds = listOf(
                SRound(
                    id = "round1",
                    pricefloor = 1.4,
                    winnerDemandId = null,
                    winnerEcpm = null,
                    demands = listOf(
                        asDemandStatNetwork("dem1", RoundStatus.UnknownAdapter),
                        asDemandStatNetwork("dem2", RoundStatus.UnknownAdapter)
                    ),
                    bidding = asDemandStatBidding(RoundStatus.BidTimeoutReached)
                ),
                SRound(
                    id = "round2",
                    pricefloor = 1.5,
                    winnerDemandId = "bi3",
                    winnerEcpm = 1.5,
                    demands = listOf(
                        asDemandStatNetwork("dem3", RoundStatus.UnknownAdapter),
                        asDemandStatNetwork("dem4", RoundStatus.UnknownAdapter)
                    ),
                    bidding = SBidding(
                        roundStatusCode = RoundStatus.Win.code,
                        demandId = "bi3",
                        bidStartTs = 0,
                        bidFinishTs = 0,
                        fillStartTs = 0,
                        fillFinishTs = 0,
                        ecpm = 1.5,
                    )
                )
            )
        )
        coVerify(exactly = 1) {
            statRequest.invoke(
                demandAd = any(),
                statsRequestBody = eq(expect)
            )
        }
    }

    @Test
    fun `it should send FAIL state`() = runTest {
        coEvery {
            statRequest(
                demandAd = any(),
                statsRequestBody = any()
            )
        } returns BaseResponse(true, null).asSuccess()

        val auctionConfig = AuctionResponse(
            auctionConfigurationId = 10,
            auctionId = "auctionId_123",
            rounds = listOf(
                Round(
                    id = "round1",
                    timeoutMs = 1000L,
                    biddingIds = listOf("bi1", "bi2"),
                    demandIds = listOf("dem1", "dem2"),
                ),
                Round(
                    id = "round2",
                    timeoutMs = 25,
                    demandIds = listOf("dem3", "dem4"),
                    biddingIds = listOf("bi3"),
                ),
            ),
            lineItems = listOf(
                LineItem(
                    demandId = Applovin,
                    pricefloor = 0.25,
                    adUnitId = "AAAA2"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 1.2235,
                    adUnitId = "admob1"
                ),
                LineItem(
                    demandId = Admob,
                    pricefloor = 2.2235,
                    adUnitId = "admob2"
                ),
            ),
            pricefloor = 0.01,
            token = null,
            externalWinNotificationsEnabled = true
        )
        testee.markAuctionStarted(auctionId = "auctionId_123")
        testee.addRoundResults(
            round = auctionConfig.rounds?.first()!!,
            pricefloor = 1.4,
            roundResults = listOf(
                AuctionResult.Network.UnknownAdapter("dem1"),
                AuctionResult.Network.UnknownAdapter("dem2"),
                AuctionResult.Bidding.Failure.TimeoutReached,
            )
        )
        testee.addRoundResults(
            round = auctionConfig.rounds[1],
            pricefloor = 1.5,
            roundResults = listOf(
                AuctionResult.Network.UnknownAdapter("dem3"),
                AuctionResult.Network.UnknownAdapter("dem4"),
                AuctionResult.Bidding.Failure.TimeoutReached,
            )
        )
        testee.sendAuctionStats(
            auctionData = auctionConfig,
            demandAd = DemandAd(AdType.Interstitial)
        )
        val expect = StatsRequestBody(
            auctionId = "auctionId_123",
            auctionConfigurationId = 10,
            result = ResultBody(
                status = "FAIL",
                demandId = null,
                ecpm = null,
                adUnitId = null,
                auctionStartTs = 1000,
                auctionFinishTs = 1000
            ),
            rounds = listOf(
                SRound(
                    id = "round1",
                    pricefloor = 1.4,
                    winnerDemandId = null,
                    winnerEcpm = null,
                    demands = listOf(
                        asDemandStatNetwork("dem1", RoundStatus.UnknownAdapter),
                        asDemandStatNetwork("dem2", RoundStatus.UnknownAdapter)
                    ),
                    bidding = asDemandStatBidding(RoundStatus.BidTimeoutReached)
                ),
                SRound(
                    id = "round2",
                    pricefloor = 1.5,
                    winnerDemandId = null,
                    winnerEcpm = null,
                    demands = listOf(
                        asDemandStatNetwork("dem3", RoundStatus.UnknownAdapter),
                        asDemandStatNetwork("dem4", RoundStatus.UnknownAdapter)
                    ),
                    bidding = asDemandStatBidding(RoundStatus.BidTimeoutReached)
                )
            )
        )
        coVerify(exactly = 1) {
            statRequest.invoke(
                demandAd = any(),
                statsRequestBody = eq(expect)
            )
        }
    }

    private fun asDemandStatNetwork(demandId: String, roundStatus: RoundStatus) = Demand(
        roundStatusCode = roundStatus.code,
        demandId = demandId,
        bidStartTs = null,
        bidFinishTs = null,
        fillStartTs = null,
        fillFinishTs = null,
        adUnitId = null,
        ecpm = null,
    )

    private fun asDemandStatBidding(roundStatus: RoundStatus) = SBidding(
        roundStatusCode = roundStatus.code,
        demandId = null,
        bidStartTs = null,
        bidFinishTs = null,
        fillStartTs = null,
        fillFinishTs = null,
        ecpm = null,
    )
}