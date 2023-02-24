package org.bidon.sdk.config.models.auctions.impl

import android.app.Activity
import android.util.Log
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapters.Process
import org.bidon.sdk.adapters.TestAdapter
import org.bidon.sdk.adapters.TestAdapterInterstitialParameters
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.RoundsListener
import org.bidon.sdk.auction.impl.AuctionImpl
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import org.bidon.sdk.auction.usecases.GetAuctionRequestUseCase
import org.bidon.sdk.base.ConcurrentTest
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.ext.asSuccess
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

private const val Applovin = "applovin"
private const val BidMachine = "bidmachine"
internal const val PlacementId = "somePlacementId"

@ExperimentalCoroutinesApi
internal class AuctionImplTest : ConcurrentTest() {

    private val activity: Activity by lazy { mockk() }
    private val getAuctionRequestUseCase: GetAuctionRequestUseCase by lazy {
        mockk(relaxed = true, relaxUnitFun = true)
    }
    private val adaptersSource: AdaptersSource by lazy { mockk(relaxed = true) }

    private val testee: Auction by lazy {
        AuctionImpl(
            adaptersSource = adaptersSource,
            getAuctionRequest = getAuctionRequestUseCase,
            statsRequest = mockk()
        )
    }

    @Before
    fun before() {
        every { adaptersSource.adapters } returns setOf(
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
//                    pricefloor = 1.0,
//                    adUnitId = "-"
//                ),
//                LineItem(
//                    demandId = Applovin,
//                    pricefloor = 0.1,
//                    adUnitId = "-"
//                ),
//                LineItem(
//                    demandId = Applovin,
//                    pricefloor = 2.0,
//                    adUnitId = "-"
//                ),
//                LineItem(
//                    demandId = Admob,
//                    pricefloor = 1.5,
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
        every { logInfo(any(), any()) } returns Unit
//        mockkStatic(::logError)
        every { logError(any(), any(), any()) } returns Unit

        coEvery {
            getAuctionRequestUseCase.request(
                placement = "asd",
                additionalData = AdTypeParam.Interstitial(activity, 1.23),
                auctionId = "123",
                adapters = mapOf()
            )
        } returns succeedAuctionConfigResponse.asSuccess()
    }

    @Ignore
    @Test
    fun `it should detect winner`() = runTest {
        val adapters = mapOf(
            "admob" to AdapterInfo("1.0", "2.0"),
            "bidmachine" to AdapterInfo("3.0", "4.0"),
        )
        println(succeedAuctionConfigResponse.asSuccess())

        getAuctionRequestUseCase.request(
            placement = "asd",
            additionalData = AdTypeParam.Interstitial(mockk(), 1.23),
            auctionId = "123",
            adapters = mapOf()
        ).onSuccess {
            println(">>" + it)
        }.onFailure {
            println(">>" + it)
        }

        val roundsListener = object : RoundsListener {
            override fun onRoundStarted(roundId: String, pricefloor: Double) {
                println("roundStarted: $roundId")
            }

            override fun onRoundSucceed(roundId: String, roundResults: List<AuctionResult>) {
                println("roundSucceed: $roundId")
            }

            override fun onRoundFailed(roundId: String, error: Throwable) {
                println("roundFailed: $roundId")
            }
        }
        testee.start(
            demandAd = DemandAd(AdType.Interstitial, PlacementId),
            roundsListener = roundsListener,
            adTypeParamData = AdTypeParam.Interstitial(activity, pricefloor = 1.0),
            resolver = MaxEcpmAuctionResolver
        ).onSuccess {
            println(it)
            error("")
        }.onFailure {
            println(it)
            error("---")
        }
    }

    private val succeedAuctionConfigResponse: AuctionResponse
        get() {
            return AuctionResponse(
                rounds = listOf(
                    Round(
                        id = "postbid",
                        timeoutMs = 15,
                        demandIds = listOf("admob", "bidmachine")
                    ),
                    Round(
                        id = "prebid",
                        timeoutMs = 25,
                        demandIds = listOf("bidmachine")
                    ),
                ),
                auctionConfigurationId = 10,
                auctionId = "auctionId_123",
                lineItems = listOf(
                    LineItem(
                        demandId = "admob",
                        pricefloor = 0.25,
                        adUnitId = "AAAA2"
                    ),
                    LineItem(
                        demandId = "bidmachine",
                        pricefloor = 1.2235,
                        adUnitId = "AAAA1"
                    ),
                ),
                fillTimeout = 10000,
                pricefloor = 0.01,
                token = null,
            )
        }
}