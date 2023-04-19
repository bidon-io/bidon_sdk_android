package org.bidon.sdk.config.models.auctions.impl

import android.app.Activity
import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.impl.AuctionImpl
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.base.ConcurrentTest
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.config.models.adapters.Process
import org.bidon.sdk.config.models.adapters.TestAdapter
import org.bidon.sdk.config.models.adapters.TestAdapterParameters
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.RoundStat
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.usecases.StatsRequestUseCase
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.di.SimpleDiStorage
import org.bidon.sdk.utils.ext.asSuccess
import org.bidon.sdk.utils.networking.BaseResponse
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val Applovin = "applovin"
private const val Admob = "admob"

@ExperimentalCoroutinesApi
internal class AuctionImplTest : ConcurrentTest() {

    private val activity: Activity by lazy { mockk() }
    private val getAuctionRequestUseCase: GetAuctionRequestUseCase = mockk()
    private val statsRequestUseCase: StatsRequestUseCase = mockk()

    private val adaptersSource: AdaptersSource by lazy { mockk(relaxed = true) }

    private val testee: Auction by lazy {
        AuctionImpl(
            adaptersSource = adaptersSource,
            getAuctionRequest = getAuctionRequestUseCase,
            statsRequest = statsRequestUseCase
        )
    }

    @Before
    fun before() {
        DI.init(activity)
        DI.setFactories()
        mockkStatic(Log::class)
        mockkStatic(::logInfo)
        every { logInfo(any(), any()) } returns Unit
        every { logInfo(any(), any()) } returns Unit
        every { logError(any(), any(), any()) } returns Unit
        coEvery {
            statsRequestUseCase.invoke(
                auctionId = any(),
                auctionConfigurationId = any(),
                results = any(),
                demandAd = any()
            )
        } returns BaseResponse(
            success = true,
            error = null
        ).asSuccess()
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
        )
        val auctionConfig = AuctionResponse(
            rounds = listOf(
                Round(
                    id = "round_1",
                    timeoutMs = 15,
                    demandIds = listOf(Applovin, Admob)
                ),
                Round(
                    id = "round_2",
                    timeoutMs = 25,
                    demandIds = listOf(Admob)
                ),
            ),
            auctionConfigurationId = 10,
            auctionId = "auctionId_123",
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
            fillTimeout = 10000,
            pricefloor = 0.01,
            token = null,
        )
        coEvery {
            getAuctionRequestUseCase.request(
                additionalData = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN 2 rounds are completed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParamData = AdTypeParam.Interstitial(activity, 1.0),
            resolver = MaxEcpmAuctionResolver
        ).onSuccess { auctionResults ->

            // THEN it should detect winner in round_2
            assertThat(auctionResults).hasSize(3)
            val winner = auctionResults.first()
            val winnerAd = winner.adSource.ad
            requireNotNull(winnerAd)
            assertThat(winnerAd.adUnitId).isEqualTo("admob2")
            assertThat(winnerAd.ecpm).isEqualTo(2.2235)
            assertThat(winnerAd.roundId).isEqualTo("round_2")
            assertThat(winner.ecpm).isEqualTo(2.2235)
            val roundStat = slot<List<RoundStat>>()
            val demandAd = slot<DemandAd>()
            // AND CHECK STAT REQUEST
            coVerify(exactly = 1) {
                statsRequestUseCase.invoke(
                    auctionId = "auctionId_123",
                    auctionConfigurationId = 10,
                    results = capture(roundStat),
                    demandAd = capture(demandAd),
                )
            }
            assertThat(demandAd.captured.adType).isEqualTo(AdType.Interstitial)
            val actualRoundStat = roundStat.captured
            // LOSERS
            assertThat(actualRoundStat[0].auctionId).isEqualTo("auctionId_123")
            assertThat(actualRoundStat[0].roundId).isEqualTo("round_1")
            assertThat(actualRoundStat[0].demands).hasSize(2)
            assertThat(actualRoundStat[0].demands[0].roundStatus).isEqualTo(RoundStatus.Loss)
            assertThat(actualRoundStat[0].demands[0].ecpm).isEqualTo(1.2235)
            assertThat(actualRoundStat[0].demands[0].fillStartTs).isNull()
            assertThat(actualRoundStat[0].demands[1].roundStatus).isEqualTo(RoundStatus.Loss)
            assertThat(actualRoundStat[0].demands[1].ecpm).isEqualTo(0.25)
            assertThat(actualRoundStat[0].demands[1].fillStartTs).isNull()
            // WINNER
            assertThat(actualRoundStat[1].auctionId).isEqualTo("auctionId_123")
            assertThat(actualRoundStat[1].roundId).isEqualTo("round_2")
            assertThat(actualRoundStat[1].demands).hasSize(1)
            assertThat(actualRoundStat[1].demands[0].roundStatus).isEqualTo(RoundStatus.Win)
            assertThat(actualRoundStat[1].demands[0].ecpm).isEqualTo(2.2235)
            assertThat(actualRoundStat[1].demands[0].adUnitId).isEqualTo("admob2")
            assertThat(actualRoundStat[1].demands[0].fillStartTs).isNotNull()
            assertThat(actualRoundStat[1].demands[0].fillFinishTs).isNotNull()
        }.onFailure {
            error("unexpected: $it")
        }
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
                additionalData = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN 2 rounds are completed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParamData = AdTypeParam.Interstitial(activity, 1.0),
            resolver = MaxEcpmAuctionResolver
        ).onSuccess { auctionResults ->

            // THEN it should detect winner in round_1. "admob2" can not fill.
            /**
             * success bid results: 1-"admob2". 2-"AAAA2". 3-"admob1"
             * success fill results: 2-"AAAA2". WINNER
             */

            assertThat(auctionResults).hasSize(2)
            val winner = auctionResults.first()
            val winnerAd = winner.adSource.ad
            requireNotNull(winnerAd)
            assertThat(winnerAd.adUnitId).isEqualTo("AAAA2")
            assertThat(winnerAd.ecpm).isEqualTo(2.25)
            assertThat(winnerAd.roundId).isEqualTo("round_1")
            assertThat(winner.ecpm).isEqualTo(2.25)
        }.onFailure {
            error("unexpected: $it")
        }
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
                additionalData = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN all bids failed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParamData = AdTypeParam.Interstitial(activity, 1.0),
            resolver = MaxEcpmAuctionResolver
        ).onSuccess {
            error("unexpected")
        }.onFailure {
            // THEN it should expose NoAuctionResults
            assertThat(it).isEqualTo(BidonError.NoAuctionResults)
        }
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
                additionalData = any(),
                auctionId = any(),
                adapters = any(),
                demandAd = any()
            )
        } returns auctionConfig.asSuccess()

        // WHEN all fills failed
        testee.start(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParamData = AdTypeParam.Interstitial(activity, 1.0),
            resolver = MaxEcpmAuctionResolver
        ).onSuccess {
            error("unexpected")
        }.onFailure {
            // THEN it should expose NoAuctionResults
            assertThat(it).isEqualTo(BidonError.NoAuctionResults)
        }
    }

    private fun getAuctionResponse() = AuctionResponse(
        rounds = listOf(
            Round(
                id = "round_1",
                timeoutMs = 15,
                demandIds = listOf(Applovin, Admob)
            ),
            Round(
                id = "round_2",
                timeoutMs = 25,
                demandIds = listOf(Admob)
            ),
        ),
        auctionConfigurationId = 10,
        auctionId = "auctionId_123",
        lineItems = listOf(
            LineItem(
                demandId = Applovin,
                pricefloor = 2.25,
                adUnitId = "AAAA2"
            ),
            LineItem(
                demandId = Admob,
                pricefloor = 1.2235,
                adUnitId = "admob1"
            ),
            LineItem(
                demandId = Admob,
                pricefloor = 3.2235,
                adUnitId = "admob2"
            ),
        ),
        fillTimeout = 10000,
        pricefloor = 0.01,
        token = null,
    )
}