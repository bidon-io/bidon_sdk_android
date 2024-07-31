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
import org.bidon.sdk.ads.BidsInfo
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.TokenInfo
import org.bidon.sdk.auction.usecases.impl.AuctionStatImpl
import org.bidon.sdk.auction.usecases.models.BiddingResult
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.mockkLog
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.ResultBody
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.models.StatsAdUnit
import org.bidon.sdk.stats.models.StatsRequestBody
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.di.SimpleDiStorage
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject
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
            adTypeParam = AdTypeParam.Interstitial(
                activity = mockk(),
                pricefloor = 1.1,
                auctionKey = null
            )
        )
        val actual = testee.addRoundResults(
            RoundResult.Results(
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    adUnits = listOf(
                        AdUnit(
                            demandId = "vungle",
                            label = "vungle_bidding_android_inter",
                            pricefloor = 1.24,
                            bidType = BidType.RTB,
                            uid = "1687107176709095424",
                            timeout = 5000,
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        )
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("vungle")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("vungle"),
                                    ecpm = 1.24,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    dspSource = "vungle",
                                    auctionPricefloor = 0.1,
                                    adUnit = AdUnit(
                                        demandId = "vungle",
                                        label = "vungle_bidding_android_inter",
                                        pricefloor = 1.24,
                                        bidType = BidType.RTB,
                                        uid = "1687107176709095424",
                                        timeout = 5000,
                                        ext = jsonObject {
                                            "payload" hasValue "payload123"
                                        }.toString()
                                    ),
                                    tokenInfo = TokenInfo(
                                        token = "token123",
                                        tokenStartTs = 678L,
                                        tokenFinishTs = 679L,
                                        status = TokenInfo.Status.SUCCESS.code,
                                    ),
                                )
                            },
                            roundStatus = RoundStatus.Successful
                        ),
                        AuctionResult.UnknownAdapter(
                            adUnit = getUnknowAdapterAdUnit(demandId = "demId7")
                        ),
                    )
                ),
                networkResults = listOf(
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("bidmachine"),
                                ecpm = 0.20,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    demandId = "bidmachine",
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 0.3,
                                    bidType = BidType.CPM,
                                    timeout = 5000,
                                    uid = "123"
                                ),
                                tokenInfo = TokenInfo(
                                    token = "token123",
                                    tokenStartTs = 678L,
                                    tokenFinishTs = 679L,
                                    status = TokenInfo.Status.SUCCESS.code,
                                ),
                            )
                            every { it.demandId } returns DemandId("bidmachine")
                        },
                        roundStatus = RoundStatus.Successful,
                    ),
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.getStats() } returns BidStat(
                                demandId = DemandId("admob"),
                                ecpm = 26.0,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                dspSource = null,
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    demandId = "admob",
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 26.0,
                                    bidType = BidType.CPM,
                                    timeout = 5000,
                                    uid = "123"
                                ),
                                tokenInfo = null,
                            )
                            every { it.demandId } returns DemandId("admob")
                        },
                        roundStatus = RoundStatus.NoFill,
                    ),
                    AuctionResult.UnknownAdapter(
                        adUnit = getUnknowAdapterAdUnit(demandId = "dem4")
                    ),
                    AuctionResult.UnknownAdapter(
                        adUnit = getUnknowAdapterAdUnit(demandId = "dem3")
                    ),
                ),
                noBidsInfo = listOf(
                    BidsInfo(
                        bidType = BidType.RTB.code,
                        demandId = "dem5",
                        label = "dem5_label",
                        price = 0.2,
                        uid = "12365",
                        ext = JSONObject()
                    ),
                    BidsInfo(
                        bidType = BidType.RTB.code,
                        demandId = "dem6",
                        label = "dem6_label",
                        price = 0.02,
                        uid = "12356",
                        ext = JSONObject()
                    )
                ),
                pricefloor = 1.1
            )
        )

        val expected = RoundStat(
            auctionId = "auction_id_123",
            pricefloor = 1.1,
            demands = listOf(
                StatsAdUnit(
                    demandId = "admob",
                    status = RoundStatus.NoFill.code,
                    price = 26.0,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    fillStartTs = 986,
                    fillFinishTs = 987,
                    adUnitUid = "123",
                    adUnitLabel = "dem1_label",
                    ext = JSONObject()
                ),
                StatsAdUnit(
                    demandId = "vungle",
                    status = RoundStatus.Win.code,
                    price = 1.24,
                    tokenStartTs = 678L,
                    tokenFinishTs = 679L,
                    bidType = BidType.RTB.code,
                    fillStartTs = 916,
                    fillFinishTs = 917,
                    adUnitLabel = "vungle_bidding_android_inter",
                    adUnitUid = "1687107176709095424",
                    ext = JSONObject()
                ),
                StatsAdUnit(
                    demandId = "bidmachine",
                    status = RoundStatus.Successful.code,
                    price = 0.2,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    fillStartTs = 986,
                    fillFinishTs = 987,
                    adUnitLabel = "dem1_label",
                    adUnitUid = "123",
                    ext = JSONObject()
                ),
                getDemandStatAdapter(demandId = "dem3", status = RoundStatus.UnknownAdapter),
                getDemandStatAdapter(demandId = "dem4", status = RoundStatus.UnknownAdapter),
                getDemandStatAdapter(demandId = "bid3", status = RoundStatus.UnknownAdapter),
            ),
            winnerEcpm = 1.24,
            noBids = listOf(
                BidsInfo(
                    bidType = BidType.RTB.code,
                    demandId = "dem5",
                    label = "dem5_label",
                    price = 0.2,
                    uid = "12365",
                    ext = JSONObject()
                ),
                BidsInfo(
                    bidType = BidType.RTB.code,
                    demandId = "dem6",
                    label = "dem6_label",
                    price = 0.02,
                    uid = "12356",
                    ext = JSONObject()
                )
            ),
            winnerDemandId = DemandId("vungle"),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `it should save results, Bidding winner`() = runTest {
        // create mock data for Bid
        testee.markAuctionStarted(
            auctionId = "auction_id_123",
            adTypeParam = AdTypeParam.Interstitial(
                activity = mockk(),
                pricefloor = 1.1,
                auctionKey = null
            )
        )
        val actual = testee.addRoundResults(
            RoundResult.Results(
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    adUnits = listOf(
                        AdUnit(
                            demandId = "bidmachine",
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString(),
                            label = "bidmachine_label",
                            pricefloor = 1.1,
                            bidType = BidType.RTB,
                            timeout = 5000,
                            uid = "1234"
                        )
                    ),
                    results = listOf(
                        AuctionResult.Bidding(
                            adSource = mockk<AdSource<*>>(relaxed = true).also {
                                every { it.demandId } returns DemandId("bidmachine")
                                every { it.getStats() } returns BidStat(
                                    demandId = DemandId("bidmachine"),
                                    ecpm = 1.5,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    dspSource = "liftoff",
                                    auctionPricefloor = 0.11,
                                    adUnit = AdUnit(
                                        bidType = BidType.RTB,
                                        demandId = "bidmachine",
                                        ext = null,
                                        label = "bidmachine_label",
                                        pricefloor = 1.5,
                                        timeout = 5000,
                                        uid = "1234"
                                    ),
                                    tokenInfo = TokenInfo(
                                        token = "token123",
                                        tokenStartTs = 678L,
                                        tokenFinishTs = 679L,
                                        status = TokenInfo.Status.SUCCESS.code,
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
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    bidType = BidType.CPM,
                                    demandId = "dem1",
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 0.3,
                                    timeout = 5000,
                                    uid = "123"
                                ),
                                tokenInfo = TokenInfo(
                                    token = "token123",
                                    tokenStartTs = 678L,
                                    tokenFinishTs = 679L,
                                    status = TokenInfo.Status.SUCCESS.code,
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
                                ecpm = 10.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    bidType = BidType.CPM,
                                    demandId = "dem2",
                                    ext = null,
                                    label = "dem2_label",
                                    pricefloor = 0.3,
                                    timeout = 5000,
                                    uid = "123"
                                ),
                                tokenInfo = TokenInfo(
                                    token = "token123",
                                    tokenStartTs = 678L,
                                    tokenFinishTs = 679L,
                                    status = TokenInfo.Status.SUCCESS.code,
                                ),
                            )
                            every { it.demandId } returns DemandId("dem2")
                        },
                        roundStatus = RoundStatus.NoFill,
                    ),
                    AuctionResult.UnknownAdapter(
                        adUnit = getUnknowAdapterAdUnit(demandId = "dem3")
                    ),
                    AuctionResult.UnknownAdapter(
                        adUnit = getUnknowAdapterAdUnit(demandId = "dem4")
                    )
                ),
                noBidsInfo = listOf(
                    BidsInfo(
                        bidType = BidType.RTB.code,
                        demandId = "dem5",
                        label = "dem5_label",
                        price = 0.2,
                        uid = "12365",
                        ext = JSONObject()
                    ),
                    BidsInfo(
                        bidType = BidType.RTB.code,
                        demandId = "dem6",
                        label = "dem6_label",
                        price = 0.02,
                        uid = "12356",
                        ext = JSONObject()
                    )
                ),
                pricefloor = 1.1
            )
        )
        val expected = RoundStat(
            auctionId = "auction_id_123",
            pricefloor = 1.1,
            demands = listOf(
                StatsAdUnit(
                    demandId = "dem2",
                    status = RoundStatus.NoFill.code,
                    price = 10.5,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    fillStartTs = 986,
                    fillFinishTs = 987,
                    adUnitUid = "123",
                    adUnitLabel = "dem2_label",
                    ext = JSONObject()
                ),
                StatsAdUnit(
                    demandId = "bidmachine",
                    status = RoundStatus.Win.code,
                    price = 1.5,
                    fillStartTs = 916,
                    fillFinishTs = 917,
                    tokenStartTs = 678L,
                    tokenFinishTs = 679L,
                    bidType = BidType.RTB.code,
                    adUnitUid = "1234",
                    adUnitLabel = "bidmachine_label",
                    ext = JSONObject()
                ),
                StatsAdUnit(
                    demandId = "dem1",
                    status = RoundStatus.Successful.code,
                    price = 1.3,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    fillStartTs = 986,
                    fillFinishTs = 987,
                    adUnitUid = "123",
                    adUnitLabel = "dem1_label",
                    ext = JSONObject()
                ),
                getDemandStatAdapter(demandId = "dem3", status = RoundStatus.UnknownAdapter),
                getDemandStatAdapter(demandId = "dem4", status = RoundStatus.UnknownAdapter),
            ),
            winnerEcpm = 1.5,
            winnerDemandId = DemandId("bidmachine"),
            noBids = listOf(
                BidsInfo(
                    bidType = BidType.RTB.code,
                    demandId = "dem5",
                    label = "dem5_label",
                    price = 0.2,
                    uid = "12365",
                    ext = JSONObject()
                ),
                BidsInfo(
                    bidType = BidType.RTB.code,
                    demandId = "dem6",
                    label = "dem6_label",
                    price = 0.02,
                    uid = "12356",
                    ext = JSONObject()
                )
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `it should send stat, Bidding wins`() = runTest {
        val systemTime = freezeTime(100500L)
        val noBids = listOf(
            AdUnit(
                bidType = BidType.RTB,
                demandId = "dem5",
                label = "dem5_label",
                pricefloor = 0.2,
                timeout = 5000L,
                uid = "12365",
                ext = ""
            ),
            AdUnit(
                bidType = BidType.RTB,
                demandId = "dem6",
                label = "dem6_label",
                pricefloor = 0.02,
                uid = "12356",
                timeout = 5000L,
                ext = ""
            )
        )
        val auctionData = AuctionResponse(
            adUnits = listOf(),
            pricefloor = 0.01,
            auctionId = "auction_id_123",
            auctionConfigurationId = 10,
            auctionConfigurationUid = "10",
            externalWinNotificationsEnabled = true,
            noBids = noBids,
            auctionTimeout = 1000L
        )
        testee.markAuctionStarted(
            auctionId = "auction_id_123",
            adTypeParam = AdTypeParam.Interstitial(
                activity = mockk(),
                pricefloor = 1.1,
                auctionKey = null,
            )
        )
        val roundStat = testee.addRoundResults(
            RoundResult.Results(
                biddingResult = BiddingResult.FilledAd(
                    serverBiddingStartTs = 28,
                    serverBiddingFinishTs = 29,
                    adUnits = listOf(
                        AdUnit(
                            demandId = "bidmachine",
                            label = "bidmachine_label",
                            pricefloor = 0.0,
                            bidType = BidType.RTB,
                            uid = "1234",
                            timeout = 5000,
                            ext = jsonObject {
                                "payload" hasValue "payload123"
                            }.toString()
                        ),
                        AdUnit(
                            demandId = "meta",
                            label = "meta_label",
                            pricefloor = 0.0,
                            bidType = BidType.RTB,
                            uid = "1232",
                            timeout = 5000,
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
                                    ecpm = 1.5,
                                    auctionId = "auction_id_123",
                                    fillStartTs = 916,
                                    fillFinishTs = 917,
                                    roundStatus = RoundStatus.Successful,
                                    dspSource = "liftoff",
                                    auctionPricefloor = 0.11,
                                    adUnit = AdUnit(
                                        demandId = "bidmachine",
                                        bidType = BidType.RTB,
                                        ext = null,
                                        label = "bidmachine_label",
                                        pricefloor = 0.1,
                                        timeout = 5000,
                                        uid = "1234"
                                    ),
                                    tokenInfo = TokenInfo(
                                        token = "token123",
                                        tokenStartTs = 678L,
                                        tokenFinishTs = 679L,
                                        status = TokenInfo.Status.SUCCESS.code,
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
                                ecpm = 1.3,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.Successful,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    demandId = "dem1",
                                    bidType = BidType.CPM,
                                    ext = null,
                                    label = "dem1_label",
                                    pricefloor = 0.3,
                                    timeout = 5000,
                                    uid = "123"
                                ),
                                tokenInfo = TokenInfo(
                                    token = "token123",
                                    tokenStartTs = 678L,
                                    tokenFinishTs = 679L,
                                    status = TokenInfo.Status.SUCCESS.code,
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
                                ecpm = 10.5,
                                auctionId = "auction_id_123",
                                fillStartTs = 986,
                                fillFinishTs = 987,
                                roundStatus = RoundStatus.NoFill,
                                dspSource = "liftoff",
                                auctionPricefloor = 0.21,
                                adUnit = AdUnit(
                                    bidType = BidType.CPM,
                                    demandId = "dem2",
                                    ext = null,
                                    label = "dem2_label",
                                    pricefloor = 0.3,
                                    timeout = 5000,
                                    uid = "123"
                                ),
                                tokenInfo = TokenInfo(
                                    token = "token123",
                                    tokenStartTs = 678L,
                                    tokenFinishTs = 679L,
                                    status = TokenInfo.Status.SUCCESS.code,
                                ),
                            )
                            every { it.demandId } returns DemandId("dem2")
                        },
                        roundStatus = RoundStatus.NoFill,
                    )
                ),
                noBidsInfo = listOf(
                    BidsInfo(
                        bidType = BidType.RTB.code,
                        demandId = "dem5",
                        label = "dem5_label",
                        price = 0.2,
                        uid = "12365",
                        ext = JSONObject()
                    ),
                    BidsInfo(
                        bidType = BidType.RTB.code,
                        demandId = "dem6",
                        label = "dem6_label",
                        price = 0.02,
                        uid = "12356",
                        ext = JSONObject()
                    )
                ),
                pricefloor = 1.1
            )
        )

        val actual = testee.sendAuctionStats(
            auctionData = auctionData,
            roundStat = roundStat,
            demandAd = DemandAd(AdType.Interstitial)
        )
        val expected = StatsRequestBody(
            auctionId = "auction_id_123",
            auctionConfigurationId = 10,
            auctionConfigurationUid = "10",
            auctionPricefloor = 0.01,
            adUnits = listOf(
                StatsAdUnit(
                    demandId = "dem2",
                    status = RoundStatus.NoFill.code,
                    price = 10.5,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    fillStartTs = 986,
                    fillFinishTs = 987,
                    adUnitUid = "123",
                    adUnitLabel = "dem2_label",
                    ext = JSONObject()
                ),
                StatsAdUnit(
                    demandId = "bidmachine",
                    status = RoundStatus.Win.code,
                    price = 1.5,
                    tokenStartTs = 678,
                    tokenFinishTs = 679,
                    bidType = BidType.RTB.code,
                    fillStartTs = 916,
                    fillFinishTs = 917,
                    adUnitUid = "1234",
                    adUnitLabel = "bidmachine_label",
                    ext = JSONObject()
                ),
                StatsAdUnit(
                    demandId = "dem1",
                    status = "LOSE",
                    price = 1.3,
                    tokenStartTs = null,
                    tokenFinishTs = null,
                    bidType = BidType.CPM.code,
                    fillStartTs = 986,
                    fillFinishTs = 987,
                    adUnitUid = "123",
                    adUnitLabel = "dem1_label",
                    ext = JSONObject()
                )
            ),
            result = ResultBody(
                status = "SUCCESS",
                winnerDemandId = "bidmachine",
                bidType = BidType.RTB.code,
                price = 1.5,
                winnerAdUnitUid = "1234",
                winnerAdUnitLabel = "bidmachine_label",
                auctionStartTs = systemTime,
                auctionFinishTs = systemTime,
                banner = null,
                interstitial = InterstitialRequest,
                rewarded = null,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun getDemandStatAdapter(demandId: String, status: RoundStatus) = StatsAdUnit(
        demandId = demandId,
        status = status.code,
        price = null,
        bidType = null,
        tokenStartTs = null,
        tokenFinishTs = null,
        fillStartTs = null,
        fillFinishTs = null,
        adUnitUid = null,
        adUnitLabel = null,
        ext = JSONObject()
    )

    private fun getUnknowAdapterAdUnit(demandId: String) =
        AdUnit(
            bidType = BidType.RTB,
            demandId = demandId,
            label = "dem7_label",
            pricefloor = 0.021,
            uid = "123567",
            timeout = 5000L,
            ext = ""
        )
}