package org.bidon.sdk.config.models.auctions.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.impl.AuctionImpl
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.AuctionStat
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.auction.usecases.impl.AuctionStatImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.config.models.adapters.Process
import org.bidon.sdk.config.models.adapters.TestAdapter
import org.bidon.sdk.config.models.adapters.TestAdapterParameters
import org.bidon.sdk.config.models.adapters.TestBiddingAdapter
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.mockkLog
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.di.SimpleDiStorage
import org.bidon.sdk.utils.ext.asSuccess
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

internal const val BidMachine = "bidmachine"
internal const val Applovin = "applovin"
internal const val Admob = "admob"

@Ignore
@ExperimentalCoroutinesApi
internal class AuctionImplTest : ConcurrentTest() {

    private val activity: Activity by lazy { mockk() }
    private val getAuctionRequestUseCase: GetAuctionRequestUseCase = mockk()

    private val adaptersSource: AdaptersSource by lazy { mockk(relaxed = true) }
    private val executeRoundUseCase: ExecuteRoundUseCase by lazy { mockk(relaxed = true) }
    private val statRequestUseCase: StatsRequestUseCase by lazy { mockk(relaxed = true) }

    private val auctionStat: AuctionStat by lazy {
        AuctionStatImpl(
            statRequestUseCase,
            resolver = MaxEcpmAuctionResolver
        )
    }

    private val testee: Auction by lazy {
        AuctionImpl(
            adaptersSource = adaptersSource,
            getAuctionRequest = getAuctionRequestUseCase,
            auctionStat = auctionStat,
            executeRound = executeRoundUseCase
        )
    }

    @Before
    fun before() {
        mockkObject(DeviceInfo)
        every { DeviceInfo.init(any()) } returns Unit
        DI.init(activity)
        mockkLog()
    }

    @After
    fun after() {
        SimpleDiStorage.instances.clear()
    }

    @Test
    fun `it should detect winner in #round_2 when 2 rounds are completed`() = runTest {

        // PREPARE
        every { adaptersSource.adapters } returns setOf(
            TestAdapter(
                demandId = DemandId(Applovin),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Succeed,
                )
            ),
            TestAdapter(
                demandId = DemandId(Admob),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Succeed,
                )
            ),
            TestBiddingAdapter(
                demandId = DemandId(BidMachine),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Succeed,
                )
            ),
        )
        val auctionConfig = AuctionResponse(
            rounds = listOf(
                RoundRequest(
                    id = "round_1",
                    timeoutMs = 15,
                    demandIds = listOf(Applovin, Admob),
                    biddingIds = listOf(),
                ),
                RoundRequest(
                    id = "ROUND_2",
                    timeoutMs = 25,
                    demandIds = listOf(Admob),
                    biddingIds = listOf(),
                ),
            ),
            adUnits = listOf(
                AdUnit(
                    label = "admob2",
                    pricefloor = 3.2235,
                    ext = null,
                    demandId = "admob",
                    bidType = BidType.CPM,
                    uid = "1",
                ),
                AdUnit(
                    label = "admob1",
                    pricefloor = 1.2235,
                    ext = null,
                    demandId = "admob",
                    bidType = BidType.CPM,
                    uid = "1",
                ),
                AdUnit(
                    label = "AAAA2",
                    pricefloor = 2.25,
                    ext = null,
                    demandId = "applovin",
                    bidType = BidType.CPM,
                    uid = "1",
                ),
            ),
            pricefloor = 0.01,
            token = null,
            auctionId = "auctionId_123",
            auctionConfigurationId = 10,
            auctionConfigurationUid = "10",
            externalWinNotificationsEnabled = true,
        )
        coEvery {
            getAuctionRequestUseCase.request(
                adTypeParam = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN 2 rounds are completed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParam = AdTypeParam.Interstitial(activity, 1.0),
            onSuccess = { auctionResults ->

                // THEN it should detect winner in round_2
                assertThat(auctionResults).hasSize(3)
                val winner = auctionResults.first()
                val winnerAd = winner.adSource.ad
                requireNotNull(winnerAd)
                assertThat(winnerAd.adUnit.label).isEqualTo("admob2")
                assertThat(winnerAd.ecpm).isEqualTo(2.2235)
                assertThat(winnerAd.roundId).isEqualTo("ROUND_2")
                assertThat(winner.adSource.getStats().ecpm).isEqualTo(2.2235)
                val roundStat = slot<List<RoundStat>>()
                val demandAd = slot<DemandAd>()
                // AND CHECK STAT REQUEST
                assertThat(demandAd.captured.adType).isEqualTo(AdType.Interstitial)
                val actualRoundStat = roundStat.captured
                // LOSERS
                assertThat(actualRoundStat[0].auctionId).isEqualTo("auctionId_123")
                assertThat(actualRoundStat[0].roundId).isEqualTo("round_1")
                assertThat(actualRoundStat[0].demands).hasSize(2)
                assertThat(actualRoundStat[0].demands[0].roundStatusCode).isEqualTo(RoundStatus.Lose.code)
                assertThat(actualRoundStat[0].demands[0].price).isEqualTo(1.2235)
                assertThat(actualRoundStat[0].demands[0].fillStartTs).isNull()
                assertThat(actualRoundStat[0].demands[1].roundStatusCode).isEqualTo(RoundStatus.Lose.code)
                assertThat(actualRoundStat[0].demands[1].price).isEqualTo(0.25)
                assertThat(actualRoundStat[0].demands[1].fillStartTs).isNull()
                // WINNER
                assertThat(actualRoundStat[1].auctionId).isEqualTo("auctionId_123")
                assertThat(actualRoundStat[1].roundId).isEqualTo("ROUND_2")
                assertThat(actualRoundStat[1].demands).hasSize(1)
                assertThat(actualRoundStat[1].demands[0].roundStatusCode).isEqualTo(RoundStatus.Win.code)
                assertThat(actualRoundStat[1].demands[0].price).isEqualTo(2.2235)
                assertThat(actualRoundStat[1].demands[0].adUnitLabel).isEqualTo("admob2")
                assertThat(actualRoundStat[1].demands[0].fillStartTs).isNotNull()
                assertThat(actualRoundStat[1].demands[0].fillFinishTs).isNotNull()
                assertThat(actualRoundStat[1].demands[0].adUnitUid).isEqualTo("1")
            },
            onFailure = {
                error("unexpected: $it")
            }
        )
    }

    @Test
    fun `it should detect winner in #round_1 when 2 rounds are completed`() = runTest {
        // PREPARE
        every { adaptersSource.adapters } returns setOf(
            TestAdapter(
                demandId = DemandId(Applovin),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Succeed,
                )
            ),
            TestAdapter(
                demandId = DemandId(Admob),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Failed,
                )
            ),
        )
        val auctionConfig = getAuctionResponse()
        coEvery {
            getAuctionRequestUseCase.request(
                adTypeParam = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN 2 rounds are completed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParam = AdTypeParam.Interstitial(activity, 1.0),
            onSuccess = { auctionResults ->

                // THEN it should detect winner in round_1. "admob2" can not fill.
                /**
                 * success bid results: 1-"admob2". 2-"AAAA2". 3-"admob1"
                 * success fill results: 2-"AAAA2". WINNER
                 */

                assertThat(auctionResults).hasSize(2)
                val winner = auctionResults.first()
                val winnerAd = winner.adSource.ad
                requireNotNull(winnerAd)
                assertThat(winnerAd.adUnit.label).isEqualTo("AAAA2")
                assertThat(winnerAd.ecpm).isEqualTo(2.25)
                assertThat(winnerAd.roundId).isEqualTo("round_1")
                assertThat(winner.adSource.getStats().ecpm).isEqualTo(2.25)
            },
            onFailure = {
                error("unexpected: $it")
            }
        )
    }

    @Test
    fun `it should expose #NoAuctionResults when all bids failed`() = runTest {
        // PREPARE
        every { adaptersSource.adapters } returns setOf(
            TestAdapter(
                demandId = DemandId(Applovin),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Failed,
                    fill = Process.Succeed,
                )
            ),
            TestAdapter(
                demandId = DemandId(Admob),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Failed,
                    fill = Process.Succeed,
                )
            ),
        )
        val auctionConfig = getAuctionResponse()
        coEvery {
            getAuctionRequestUseCase.request(
                adTypeParam = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN all bids failed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParam = AdTypeParam.Interstitial(activity, 1.0),
            onSuccess = {
                error("unexpected")
            },
            onFailure = {
                // THEN it should expose NoAuctionResults
                assertThat(it).isEqualTo(BidonError.NoAuctionResults)
            }
        )
    }

    @Test
    fun `it should expose #NoAuctionResults when all fills failed`() = runTest {
        // PREPARE
        every { adaptersSource.adapters } returns setOf(
            TestAdapter(
                demandId = DemandId(Applovin),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Failed,
                )
            ),
            TestAdapter(
                demandId = DemandId(Admob),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Failed,
                )
            ),
        )
        val auctionConfig = getAuctionResponse()
        coEvery {
            getAuctionRequestUseCase.request(
                adTypeParam = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN all fills failed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParam = AdTypeParam.Interstitial(activity, 1.0),
            onSuccess = {
                error("unexpected")
            },
            onFailure = {
                // THEN it should expose NoAuctionResults
                assertThat(it).isEqualTo(BidonError.NoAuctionResults)
            }
        )
    }

    private fun getAuctionResponse() = AuctionResponse(
        rounds = listOf(
            RoundRequest(
                id = "round_1",
                timeoutMs = 15,
                demandIds = listOf(Applovin, Admob),
                biddingIds = listOf(),
            ),
            RoundRequest(
                id = "ROUND_2",
                timeoutMs = 25,
                demandIds = listOf(Admob),
                biddingIds = listOf(),
            ),
        ),
        adUnits = listOf(
            AdUnit(
                label = "admob2",
                pricefloor = 3.2235,
                ext = null,
                demandId = "admob",
                bidType = BidType.CPM,
                uid = "1",
            ),
            AdUnit(
                label = "admob1",
                pricefloor = 1.2235,
                ext = null,
                demandId = "admob",
                bidType = BidType.CPM,
                uid = "1",
            ),
            AdUnit(
                label = "AAAA2",
                pricefloor = 2.25,
                ext = null,
                demandId = "applovin",
                bidType = BidType.CPM,
                uid = "1",
            ),

        ),
        pricefloor = 0.01,
        token = null,
        auctionId = "auctionId_123",
        auctionConfigurationId = 10,
        auctionConfigurationUid = "10",
        externalWinNotificationsEnabled = true,
    )
}