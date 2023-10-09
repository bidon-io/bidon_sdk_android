package org.bidon.sdk.auction.usecases

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.impl.AuctionStatImpl
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.mockkLog
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.DemandStat
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.di.SimpleDiStorage
import org.bidon.sdk.utils.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class AuctionStatImplTest : ConcurrentTest() {

    private val statRequestUseCase: StatsRequestUseCase = mockk(relaxed = true)
    private val testee: AuctionStat by lazy {
        AuctionStatImpl(
            statsRequest = statRequestUseCase,
            resolver = MaxEcpmAuctionResolver
        )
    }

    @Before
    fun before() {
        mockkObject(DeviceInfo)
        every { DeviceInfo.init(any()) } returns Unit
        DI.init(mockk())
//        DI.setFactories()
        mockkLog()
    }

    @After
    fun after() {
        unmockkAll()
        SimpleDiStorage.instances.clear()
    }

    @Test
    fun `it should save results, DSP winner`() = runTest {
        // create mock data for Bid
        testee.markAuctionStarted(auctionId = "auction_id_123")
        val actual = testee.addRoundResults(
            RoundResult.Results(
                round = RoundRequest(
                    id = "ROUND_1",
                    timeoutMs = 1000L,
                    demandIds = listOf("dem1", "dem2", "dem3", "dem4"),
                    biddingIds = listOf("bidmachine", "meta", "bid3", "bid4")
                ),
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    bids = listOf(
                        BidResponse(
                            id = "bidmachine",
                            impressionId = "imp1",
                            price = 1.2,
                            demands = listOf(
                                "bidmachine" to jsonObject {
                                    "payload" hasValue "payload123"
                                }
                            )
                        ),
                        BidResponse(
                            id = "meta",
                            impressionId = "imp1",
                            price = 1.15,
                            demands = listOf(
                                "meta" to jsonObject {
                                    "payload" hasValue "payload123"
                                }
                            )
                        )
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    adUnitId = null,
                                    roundId = "ROUND_1",
                                    ecpm = 1.2,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    bidType = BidType.RTB,
                                    lineItemUid = null,
                                    dspSource = "liftoff",
                                )
                            },
                            roundStatus = RoundStatus.Successful
                        ),
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("meta")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("meta"),
                                    adUnitId = null,
                                    roundId = "ROUND_1",
                                    ecpm = 1.15,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    bidType = BidType.RTB,
                                    lineItemUid = null,
                                    dspSource = "liftoff",
                                )
                            },
                            roundStatus = RoundStatus.Successful
                        ),
                        AuctionResult.UnknownAdapter(
                            adapterName = "bid3",
                            type = AuctionResult.UnknownAdapter.Type.Bidding
                        ),
                    )
                ),
                networkResults = listOf(
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem1"),
                                adUnitId = "ad_unit_id_123",
                                roundId = "ROUND_1",
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                roundIndex = 2,
                                bidType = BidType.CPM,
                                lineItemUid = "123",
                                dspSource = "liftoff",
                            )
                            every { it.demandId } returns DemandId("dem1")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem2"),
                                adUnitId = "ad_unit_id_123",
                                roundId = "ROUND_1",
                                ecpm = 1.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                roundIndex = 2,
                                bidType = BidType.CPM,
                                lineItemUid = "123",
                                dspSource = "liftoff",
                            )
                            every { it.demandId } returns DemandId("dem2")
                        },
                        roundStatus = RoundStatus.NoFill,
                    ),
                    AuctionResult.UnknownAdapter(
                        adapterName = "dem3",
                        type = AuctionResult.UnknownAdapter.Type.Network
                    ),
                    AuctionResult.UnknownAdapter(
                        adapterName = "dem4",
                        type = AuctionResult.UnknownAdapter.Type.Network
                    ),
                ),
                pricefloor = 1.1
            )
        )

        assertThat(actual).isEqualTo(
            listOf<RoundStat>(
                RoundStat(
                    auctionId = "auction_id_123",
                    roundId = "ROUND_1",
                    pricefloor = 1.1,
                    demands = listOf(
                        DemandStat.Network(
                            demandId = "dem1",
                            adUnitId = "ad_unit_id_123",
                            roundStatusCode = RoundStatus.Successful.code,
                            ecpm = 1.3,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            lineItemUid = "123",
                        ),
                        DemandStat.Network(
                            demandId = "dem2",
                            adUnitId = "ad_unit_id_123",
                            roundStatusCode = RoundStatus.NoFill.code,
                            ecpm = 1.5,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            lineItemUid = "123",
                        ),
                        getDemandStatAdapter("dem3", RoundStatus.UnknownAdapter),
                        getDemandStatAdapter("dem4", RoundStatus.UnknownAdapter),
                    ),
                    bidding = DemandStat.Bidding(
                        bidStartTs = 28,
                        bidFinishTs = 29,
                        bids = listOf(
                            DemandStat.Bidding.Bid(
                                roundStatusCode = RoundStatus.Successful.code,
                                demandId = "bidmachine",
                                ecpm = 1.2,
                                fillStartTs = 916,
                                fillFinishTs = 917
                            ),
                            DemandStat.Bidding.Bid(
                                roundStatusCode = RoundStatus.Successful.code,
                                demandId = "meta",
                                ecpm = 1.15,
                                fillStartTs = 916,
                                fillFinishTs = 917
                            ),
                            getBiddingStatAdapter("bid3", RoundStatus.UnknownAdapter),
                        )
                    ),
                    winnerDemandId = DemandId(demandId = "dem1"),
                    winnerEcpm = 1.3
                )
            )
        )
    }

    @Test
    fun `it should save results, Bidding winner`() = runTest {
        // create mock data for Bid
        testee.markAuctionStarted(auctionId = "auction_id_123")
        val actual = testee.addRoundResults(
            RoundResult.Results(
                round = RoundRequest(
                    id = "ROUND_1",
                    timeoutMs = 1000L,
                    demandIds = listOf("dem1", "dem2", "dem3", "dem4"),
                    biddingIds = listOf("bidmachine", "bid2", "bid3", "bid4")
                ),
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    bids = listOf(
                        BidResponse(
                            id = "bidmachine",
                            impressionId = "imp1",
                            price = 1.5,
                            demands = listOf(
                                "bidmachine" to jsonObject {
                                    "payload" hasValue "payload123"
                                }
                            )
                        )
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    adUnitId = null,
                                    roundId = "ROUND_1",
                                    ecpm = 1.5,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    bidType = BidType.RTB,
                                    lineItemUid = "123",
                                    dspSource = "liftoff",
                                )
                            },
                            roundStatus = RoundStatus.Successful
                        ),
                    )
                ),
                networkResults = listOf(
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem1"),
                                adUnitId = "ad_unit_id_123",
                                roundId = "ROUND_1",
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                roundIndex = 2,
                                bidType = BidType.CPM,
                                lineItemUid = "123",
                                dspSource = "liftoff",
                            )
                            every { it.demandId } returns DemandId("dem1")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem2"),
                                adUnitId = "ad_unit_id_123",
                                roundId = "ROUND_1",
                                ecpm = 10.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                roundIndex = 2,
                                bidType = BidType.CPM,
                                lineItemUid = "123",
                                dspSource = "liftoff",
                            )
                            every { it.demandId } returns DemandId("dem2")
                        },
                        roundStatus = RoundStatus.NoFill,
                    ),
                    AuctionResult.UnknownAdapter(
                        adapterName = "dem3",
                        type = AuctionResult.UnknownAdapter.Type.Network
                    ),
                    AuctionResult.UnknownAdapter(
                        adapterName = "dem4",
                        type = AuctionResult.UnknownAdapter.Type.Network
                    )
                ),
                pricefloor = 1.1
            )
        )

        assertThat(actual).isEqualTo(
            listOf<RoundStat>(
                RoundStat(
                    auctionId = "auction_id_123",
                    roundId = "ROUND_1",
                    pricefloor = 1.1,
                    bidding = DemandStat.Bidding(
                        bidStartTs = 28,
                        bidFinishTs = 29,
                        bids = listOf(
                            DemandStat.Bidding.Bid(
                                roundStatusCode = RoundStatus.Successful.code,
                                demandId = "bidmachine",
                                ecpm = 1.5,
                                fillStartTs = 916,
                                fillFinishTs = 917
                            ),
                        )
                    ),
                    demands = listOf(
                        DemandStat.Network(
                            demandId = "dem1",
                            adUnitId = "ad_unit_id_123",
                            roundStatusCode = RoundStatus.Successful.code,
                            ecpm = 1.3,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            lineItemUid = "123",
                        ),
                        DemandStat.Network(
                            demandId = "dem2",
                            adUnitId = "ad_unit_id_123",
                            roundStatusCode = RoundStatus.NoFill.code,
                            ecpm = 10.5,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            lineItemUid = "123",
                        ),
                        getDemandStatAdapter("dem3", RoundStatus.UnknownAdapter),
                        getDemandStatAdapter("dem4", RoundStatus.UnknownAdapter),
                    ),
                    winnerDemandId = DemandId(demandId = "bidmachine"),
                    winnerEcpm = 1.5,
                )

            )
        )
    }

    @Test
    fun `it should send stat, Bidding wins`() = runTest {
        val systemTime = freezeTime(100500L)
        val auctionData = AuctionResponse(
            rounds = listOf(
                RoundRequest(
                    id = "ROUND_1",
                    timeoutMs = 1000L,
                    demandIds = listOf("dem1", "dem2", "dem3", "dem4"),
                    biddingIds = listOf("bidmachine", "bid2", "bid3", "bid4")
                ),
                RoundRequest(
                    id = "ROUND_2",
                    timeoutMs = 25,
                    demandIds = listOf("dem1", "dem2", "dem3"),
                    biddingIds = listOf("bid2", "bid3"),
                ),
            ),
            auctionConfigurationId = 10,
            auctionId = "auction_id_123",
            lineItems = listOf(),
            pricefloor = 0.01,
            token = null,
            externalWinNotificationsEnabled = true,
            auctionConfigurationUid = "10",
        )
        testee.markAuctionStarted(auctionId = "auction_id_123")
        testee.addRoundResults(
            RoundResult.Results(
                round = auctionData.rounds!!.first(),
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    bids = listOf(
                        BidResponse(
                            id = "bidmachine",
                            impressionId = "imp1",
                            price = 1.5,
                            demands = listOf(
                                "bidmachine" to jsonObject {
                                    "payload" hasValue "payload123"
                                }
                            )
                        ),
                        BidResponse(
                            id = "bid2",
                            impressionId = "imp1",
                            price = 1.5,
                            demands = listOf(
                                "bid2" to jsonObject {
                                    "payload" hasValue "payload123"
                                }
                            )
                        ),
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    adUnitId = null,
                                    roundId = "ROUND_1",
                                    ecpm = 1.5,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    bidType = BidType.RTB,
                                    lineItemUid = null,
                                    dspSource = "liftoff",
                                )
                            },
                            roundStatus = RoundStatus.Successful
                        ),
                    )
                ),
                networkResults = listOf(
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem1"),
                                adUnitId = "ad_unit_id_123",
                                roundId = "ROUND_1",
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                roundIndex = 2,
                                bidType = BidType.CPM,
                                lineItemUid = "123",
                                dspSource = "liftoff",
                            )
                            every { it.demandId } returns DemandId("dem1")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem2"),
                                adUnitId = "ad_unit_id_123",
                                roundId = "ROUND_1",
                                ecpm = 10.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                roundIndex = 2,
                                bidType = BidType.CPM,
                                lineItemUid = "123",
                                dspSource = "liftoff",
                            )
                            every { it.demandId } returns DemandId("dem2")
                        },
                        roundStatus = RoundStatus.NoFill,
                    )
                ),
                pricefloor = 1.1
            )
        )

        val actual = testee.sendAuctionStats(
            auctionData = auctionData,
            demandAd = DemandAd(AdType.Interstitial)
        )
        assertThat(actual).isEqualTo(
            StatsRequestBody(
                auctionId = "auction_id_123", auctionConfigurationId = 10,
                rounds = listOf(
                    org.bidon.sdk.stats.models.Round(
                        id = "ROUND_1",
                        pricefloor = 1.1,
                        winnerDemandId = "bidmachine",
                        winnerEcpm = 1.5,
                        demands = listOf(
                            DemandStat.Network(
                                demandId = "dem1",
                                adUnitId = "ad_unit_id_123",
                                roundStatusCode = "LOSE",
                                ecpm = 1.3,
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                lineItemUid = "123",
                            ),
                            DemandStat.Network(
                                demandId = "dem2",
                                adUnitId = "ad_unit_id_123",
                                roundStatusCode = "NO_FILL",
                                ecpm = 10.5,
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                lineItemUid = "123",
                            )
                        ),
                        bidding = DemandStat.Bidding(
                            bidStartTs = 28,
                            bidFinishTs = 29,
                            bids = listOf(
                                DemandStat.Bidding.Bid(
                                    roundStatusCode = RoundStatus.Win.code,
                                    demandId = "bidmachine",
                                    ecpm = 1.5,
                                    fillStartTs = 916,
                                    fillFinishTs = 917
                                ),
                            ),
                        )
                    ),
                    org.bidon.sdk.stats.models.Round(
                        id = "ROUND_2",
                        demands = listOf(
                            getDemandStatAdapter("dem1", RoundStatus.AuctionCancelled),
                            getDemandStatAdapter("dem2", RoundStatus.AuctionCancelled),
                            getDemandStatAdapter("dem3", RoundStatus.AuctionCancelled),
                        ),
                        bidding = DemandStat.Bidding(
                            bidStartTs = null,
                            bidFinishTs = null,
                            bids = listOf(
                                DemandStat.Bidding.Bid(
                                    roundStatusCode = RoundStatus.AuctionCancelled.code,
                                    demandId = null,
                                    ecpm = null,
                                    fillStartTs = null,
                                    fillFinishTs = null
                                ),
                            ),
                        ),
                        pricefloor = null,
                        winnerDemandId = null,
                        winnerEcpm = null
                    )
                ),
                result = ResultBody(
                    status = "SUCCESS",
                    roundId = "ROUND_1",
                    demandId = "bidmachine",
                    ecpm = 1.5,
                    adUnitId = null,
                    auctionStartTs = systemTime,
                    auctionFinishTs = systemTime,
                    bidType = BidType.RTB.code,
                    lineItemUid = null,
                ),
                auctionConfigurationUid = "10",
            )
        )
    }

    private fun getDemandStatAdapter(demandId: String, status: RoundStatus) = DemandStat.Network(
        demandId = demandId,
        adUnitId = null,
        roundStatusCode = status.code,
        ecpm = null,
        fillStartTs = null,
        fillFinishTs = null,
        lineItemUid = null,
    )

    private fun getBiddingStatAdapter(demandId: String, status: RoundStatus) = DemandStat.Bidding.Bid(
        demandId = demandId,
        roundStatusCode = status.code,
        ecpm = null,
        fillStartTs = null,
        fillFinishTs = null
    )
}