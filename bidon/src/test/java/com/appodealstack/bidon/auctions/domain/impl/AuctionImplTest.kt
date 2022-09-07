package com.appodealstack.bidon.auctions.domain.impl

import android.app.Activity
import android.util.Log
import com.appodealstack.bidon.adapters.Process
import com.appodealstack.bidon.adapters.TestAdapter
import com.appodealstack.bidon.adapters.TestAdapterInterstitialParameters
import com.appodealstack.bidon.base.ConcurrentTest
import com.appodealstack.bidon.domain.adapter.AdaptersSource
import com.appodealstack.bidon.domain.auction.AdTypeParam
import com.appodealstack.bidon.domain.auction.Auction
import com.appodealstack.bidon.domain.auction.AuctionResult
import com.appodealstack.bidon.domain.auction.RoundsListener
import com.appodealstack.bidon.domain.auction.impl.AuctionImpl
import com.appodealstack.bidon.domain.auction.impl.MaxEcpmAuctionResolver
import com.appodealstack.bidon.domain.auction.usecases.GetAuctionRequestUseCase
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.common.DemandAd
import com.appodealstack.bidon.domain.stats.impl.logError
import com.appodealstack.bidon.domain.stats.impl.logInfo
import com.appodealstack.bidon.domain.stats.impl.logInternal
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

private const val Applovin = "applovin"
private const val Admob = "admob"
private const val BidMachine = "bidmachine"
internal const val PlacementId = "somePlacementId"

internal class AuctionImplTest : ConcurrentTest() {

    private val activity: Activity by lazy { mockk() }
    private val getAuctionRequestUseCase: GetAuctionRequestUseCase by lazy { mockk() }
    private val adaptersSource: AdaptersSource by lazy { mockk() }

    private val testee: Auction by lazy {
        AuctionImpl(
            adaptersSource = adaptersSource,
            getAuctionRequest = getAuctionRequestUseCase,
            statsRequest = mockk()
        )
    }

    @Before
    fun before() {
        every { adaptersSource.adapters } returns listOf(
            TestAdapter(
                demandName = Applovin,
                interstitialData = TestAdapterInterstitialParameters(
                    auctionParam = Process.Succeed
                )
            ),
            TestAdapter(
                demandName = BidMachine,
                interstitialData = TestAdapterInterstitialParameters(
                    auctionParam = Process.Succeed
                )
            ),
        )
//        coEvery {
//            getAuctionRequestUseCase.request(
//                demandAd.placement,
//                demandAd.adType,
//                adTypeAdditionalData,
//                adaptersSource.adapters.associate {
//                    it.demandId.demandId to it.adapterInfo
//                }
//            )
//        } returns AuctionResponse(
//            rounds = listOf(
//                Round(
//                    id = "round1",
//                    timeoutMs = 5000,
//                    demandIds = listOf(Applovin, Admob, BidMachine)
//                ),
//                Round(
//                    id = "round2",
//                    timeoutMs = 5000,
//                    demandIds = listOf(Admob, BidMachine)
//                ),
//                Round(
//                    id = "round3",
//                    timeoutMs = 5000,
//                    demandIds = listOf("UnknownDemandId")
//                ),
//            ),
//            fillTimeout = 5000,
//            lineItems = listOf(
//                LineItem(
//                    demandId = Applovin,
//                    priceFloor = 1.0,
//                    adUnitId = "-"
//                ),
//                LineItem(
//                    demandId = Applovin,
//                    priceFloor = 0.1,
//                    adUnitId = "-"
//                ),
//                LineItem(
//                    demandId = Applovin,
//                    priceFloor = 2.0,
//                    adUnitId = "-"
//                ),
//                LineItem(
//                    demandId = Admob,
//                    priceFloor = 1.5,
//                    adUnitId = "-"
//                ),
//            ),
//            minPrice = 1.0,
//            token = JsonObject(mapOf())
//        ).asSuccess()

        mockkStatic(Log::class)
        mockkStatic(::logInfo)
        every { logInfo(any(), any()) } returns Unit
//        mockkStatic(::logInternal)
        every { logInternal(any(), any(), any()) } returns Unit
//        mockkStatic(::logError)
        every { logError(any(), any(), any()) } returns Unit
    }

    @Test
    fun `it should`() = runTest {
//        coEvery {
//            getAuctionRequestUseCase.request(
//                demandAd.placement, demandAd.adType, adTypeAdditionalData,
//                adapters.associate {
//                    it.demandId.demandId to it.adapterInfo
//                }
//            )
//        } returns BidonError.NoAuctionResults.asFailure()
        val roundsListener = object : RoundsListener {
            override fun roundStarted(roundId: String) {
                println("roundStarted: $roundId")
            }

            override fun roundSucceed(roundId: String, roundResults: List<AuctionResult>) {
                println("roundSucceed: $roundId")
            }

            override fun roundFailed(roundId: String, error: Throwable) {
                println("roundFailed: $roundId")
            }
        }
        testee.start(
            demandAd = DemandAd(AdType.Interstitial, PlacementId),
            roundsListener = roundsListener,
            adTypeParamData = AdTypeParam.Interstitial(activity),
            resolver = MaxEcpmAuctionResolver
        ).onSuccess {
            println(it)
        }.onFailure {
            println(it)
        }
    }
}