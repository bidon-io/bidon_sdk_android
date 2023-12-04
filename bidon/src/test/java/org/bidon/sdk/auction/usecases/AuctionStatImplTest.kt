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
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.BidResponse
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.impl.AuctionStatImpl
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.mockkLog
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.DemandStat
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
        testee.markAuctionStarted(
            auctionId = "auction_id_123",
            adTypeParam = AdTypeParam.Interstitial(activity = mockk(), pricefloor = 1.1)
        )
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
                            id = "bid123",
                            impressionId = "imp1",
                            price = 1.2,
                            adUnit = AdUnit(
                                demandId = "bidmachine",
                                ext = null,
                                label = "bidmachine_label",
                                pricefloor = null,
                                bidType = BidType.CPM,
                                uid = "123"
                            ),
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        ),
                        BidResponse(
                            id = "bid2343",
                            impressionId = "imp2",
                            price = 1.15,
                            adUnit = AdUnit(
                                demandId = "meta",
                                ext = null,
                                label = "meta_label",
                                pricefloor = null,
                                bidType = BidType.CPM,
                                uid = "123"
                            ),
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        )
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    roundId = "ROUND_1",
                                    ecpm = 1.2,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    dspSource = "liftoff",
                                    auctionPricefloor = 0.1,
                                    roundPricefloor = 0.11,
                                    adUnit = AdUnit(
                                        demandId = "bidmachine",
                                        ext = null,
                                        label = "bidmachine_label",
                                        pricefloor = null,
                                        bidType = BidType.RTB,
                                        uid = "123"
                                    ),
                                )
                            },
                            roundStatus = RoundStatus.Successful
                        ),
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("meta")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("meta"),
                                    adUnit = AdUnit(
                                        demandId = "meta",
                                        ext = null,
                                        label = "meta_label",
                                        pricefloor = null,
                                        bidType = BidType.RTB,
                                        uid = "123"
                                    ),
                                    roundId = "ROUND_1",
                                    ecpm = 1.15,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    dspSource = "liftoff",
                                    auctionPricefloor = 0.1,
                                    roundPricefloor = 0.11,
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
                                roundId = "ROUND_1",
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                roundIndex = 2,
                                dspSource = "liftoff",
                                roundPricefloor = 0.22,
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    demandId = "dem1",
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 0.3,
                                    bidType = BidType.CPM,
                                    uid = "123"
                                ),
                            )
                            every { it.demandId } returns DemandId("dem1")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem2"),
                                roundId = "ROUND_1",
                                ecpm = 1.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                roundIndex = 2,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                roundPricefloor = 0.22,
                                adUnit = AdUnit(
                                    demandId = "dem2",
                                    ext = null,
                                    label = "dem2_label",
                                    pricefloor = 0.3,
                                    bidType = BidType.CPM,
                                    uid = "123"
                                ),
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
                            roundStatusCode = RoundStatus.Successful.code,
                            price = 1.3,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            adUnitUid = "123",
                            adUnitLabel = "dem1_label",
                        ),
                        DemandStat.Network(
                            demandId = "dem2",
                            roundStatusCode = RoundStatus.NoFill.code,
                            price = 1.5,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            adUnitLabel = "dem2_label",
                            adUnitUid = "123",
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
                                price = 1.2,
                                fillStartTs = 916,
                                fillFinishTs = 917,
                                adUnitLabel = "bidmachine_label",
                                adUnitUid = "123",
                            ),
                            DemandStat.Bidding.Bid(
                                roundStatusCode = RoundStatus.Successful.code,
                                demandId = "meta",
                                price = 1.15,
                                fillStartTs = 916,
                                fillFinishTs = 917,
                                adUnitLabel = "meta_label",
                                adUnitUid = "123",
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
        testee.markAuctionStarted(
            auctionId = "auction_id_123",
            adTypeParam = AdTypeParam.Interstitial(activity = mockk(), pricefloor = 1.1)
        )
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
                            id = "bid123",
                            impressionId = "imp1",
                            price = 1.5,
                            adUnit = AdUnit(
                                demandId = "bidmachine",
                                ext = null,
                                label = "bidmachine_label",
                                pricefloor = null,
                                bidType = BidType.RTB,
                                uid = "123"
                            ),
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        ),
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    roundId = "ROUND_1",
                                    ecpm = 1.5,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    dspSource = "liftoff",
                                    roundPricefloor = 0.1,
                                    auctionPricefloor = 0.11,
                                    adUnit = AdUnit(
                                        bidType = BidType.RTB,
                                        demandId = "bidmachine",
                                        ext = null,
                                        label = "bidmachine_label",
                                        pricefloor = null,
                                        uid = "123"
                                    ),
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
                                roundId = "ROUND_1",
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                roundIndex = 2,
                                dspSource = "liftoff",
                                roundPricefloor = 0.22,
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    bidType = BidType.CPM,
                                    demandId = "dem1",
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 0.3,
                                    uid = "123"
                                ),
                            )
                            every { it.demandId } returns DemandId("dem1")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem2"),
                                roundId = "ROUND_1",
                                ecpm = 10.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                roundIndex = 2,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                roundPricefloor = 0.22,
                                adUnit = AdUnit(
                                    bidType = BidType.CPM,
                                    demandId = "dem2",
                                    ext = null,
                                    label = "dem2_label",
                                    pricefloor = 0.3,
                                    uid = "123"
                                ),
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
                                price = 1.5,
                                fillStartTs = 916,
                                fillFinishTs = 917,
                                adUnitLabel = "bidmachine_label",
                                adUnitUid = "123",
                            ),
                        )
                    ),
                    demands = listOf(
                        DemandStat.Network(
                            demandId = "dem1",
                            roundStatusCode = RoundStatus.Successful.code,
                            price = 1.3,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            adUnitLabel = "dem1_label",
                            adUnitUid = "123",
                        ),
                        DemandStat.Network(
                            demandId = "dem2",
                            roundStatusCode = RoundStatus.NoFill.code,
                            price = 10.5,
                            fillStartTs = 986,
                            fillFinishTs = 987,
                            adUnitLabel = "dem2_label",
                            adUnitUid = "123",
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
            auctionId = "auction_id_123",
            pricefloor = 0.01,
            token = null,
            externalWinNotificationsEnabled = true,
            auctionConfigurationUid = "10",
            adUnits = null,
        )
        testee.markAuctionStarted(
            auctionId = "auction_id_123",
            adTypeParam = AdTypeParam.Interstitial(activity = mockk(), pricefloor = 1.1)
        )
        testee.addRoundResults(
            RoundResult.Results(
                round = auctionData.rounds!!.first(),
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    bids = listOf(
                        BidResponse(
                            id = "bid123",
                            impressionId = "imp1",
                            price = 1.5,
                            adUnit = AdUnit(
                                demandId = "bidmachine",
                                ext = null,
                                label = "bidmachine_label",
                                pricefloor = null,
                                bidType = BidType.RTB,
                                uid = "1234"
                            ),
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        ),
                        BidResponse(
                            id = "bid2343",
                            impressionId = "imp2",
                            price = 1.15,
                            adUnit = AdUnit(
                                demandId = "meta",
                                ext = null,
                                label = "meta_label",
                                pricefloor = null,
                                bidType = BidType.RTB,
                                uid = "1232"
                            ),
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        )
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    roundId = "ROUND_1",
                                    ecpm = 1.5,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    roundIndex = 2,
                                    dspSource = "liftoff",
                                    roundPricefloor = 0.1,
                                    auctionPricefloor = 0.11,
                                    adUnit = AdUnit(
                                        demandId = "bidmachine",
                                        bidType = BidType.RTB,
                                        ext = null,
                                        label = "bidmachine_label",
                                        pricefloor = null,
                                        uid = "1234"
                                    ),
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
                                roundId = "ROUND_1",
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                roundIndex = 2,
                                dspSource = "liftoff",
                                roundPricefloor = 0.22,
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    demandId = "dem1",
                                    bidType = BidType.CPM,
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 0.3,
                                    uid = "123"
                                ),
                            )
                            every { it.demandId } returns DemandId("dem1")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("dem2"),
                                roundId = "ROUND_1",
                                ecpm = 10.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                roundIndex = 2,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                roundPricefloor = 0.22,
                                adUnit = AdUnit(
                                    bidType = BidType.CPM,
                                    demandId = "dem2",
                                    ext = null,
                                    label = "dem2_label",
                                    pricefloor = 0.3,
                                    uid = "123"
                                ),
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
                auctionId = "auction_id_123",
                rounds = listOf(
                    org.bidon.sdk.stats.models.Round(
                        id = "ROUND_1",
                        pricefloor = 1.1,
                        winnerDemandId = "bidmachine",
                        winnerEcpm = 1.5,
                        demands = listOf(
                            DemandStat.Network(
                                demandId = "dem1",
                                roundStatusCode = "LOSE",
                                price = 1.3,
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                adUnitLabel = "dem1_label",
                                adUnitUid = "123",
                            ),
                            DemandStat.Network(
                                demandId = "dem2",
                                roundStatusCode = "NO_FILL",
                                price = 10.5,
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                adUnitLabel = "dem2_label",
                                adUnitUid = "123",
                            )
                        ),
                        bidding = DemandStat.Bidding(
                            bidStartTs = 28,
                            bidFinishTs = 29,
                            bids = listOf(
                                DemandStat.Bidding.Bid(
                                    roundStatusCode = RoundStatus.Win.code,
                                    demandId = "bidmachine",
                                    price = 1.5,
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    adUnitLabel = "bidmachine_label",
                                    adUnitUid = "1234",
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
                                    price = null,
                                    fillStartTs = null,
                                    fillFinishTs = null,
                                    adUnitLabel = null,
                                    adUnitUid = null,
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
                    price = 1.5,
                    auctionStartTs = systemTime,
                    auctionFinishTs = systemTime,
                    bidType = BidType.RTB.code,
                    winnerAdUnitUid = "1234",
                    winnerAdUnitLabel = "bidmachine_label",
                    winnerDemandId = "bidmachine",
                    interstitial = InterstitialRequest,
                    rewarded = null,
                    banner = null
                ),
                auctionConfigurationUid = "10",
            )
        )
    }

    private fun getDemandStatAdapter(demandId: String, status: RoundStatus) = DemandStat.Network(
        demandId = demandId,
        roundStatusCode = status.code,
        price = null,
        fillStartTs = null,
        fillFinishTs = null,
        adUnitUid = null,
        adUnitLabel = null,
    )

    private fun getBiddingStatAdapter(demandId: String, status: RoundStatus) = DemandStat.Bidding.Bid(
        demandId = demandId,
        roundStatusCode = status.code,
        price = null,
        fillStartTs = null,
        fillFinishTs = null,
        adUnitUid = null,
        adUnitLabel = null,
    )
}