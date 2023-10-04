package org.bidon.sdk.auction.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.ext.ad
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.DeviceType
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.ConductBiddingRoundUseCase
import org.bidon.sdk.auction.usecases.ConductNetworkRoundUseCase
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.impl.ExecuteRoundUseCaseImpl
import org.bidon.sdk.auction.usecases.models.NetworksResult
import org.bidon.sdk.config.models.adapters.Process
import org.bidon.sdk.config.models.adapters.TestAdapter
import org.bidon.sdk.config.models.adapters.TestAdapterParameters
import org.bidon.sdk.config.models.adapters.TestBiddingAdapter
import org.bidon.sdk.config.models.auctions.impl.Admob
import org.bidon.sdk.config.models.auctions.impl.Applovin
import org.bidon.sdk.config.models.auctions.impl.BidMachine
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.logs.analytic.AdValue.Companion.USD
import org.bidon.sdk.mockkLog
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.mainDispatcherOverridden
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 26/06/2023.
 */
internal class ExecuteRoundUseCaseImplTest : ConcurrentTest() {

    private val auctionConfig = AuctionResponse(
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
        auctionConfigurationId = 10,
        auctionId = "auctionId_123",
        lineItems = listOf(
            LineItem(
                demandId = Applovin,
                pricefloor = 0.25,
                adUnitId = "AAAA2",
                uid = "1",
            ),
            LineItem(
                demandId = Admob,
                pricefloor = 1.2235,
                adUnitId = "admob1",
                uid = "1",
            ),
            LineItem(
                demandId = Admob,
                pricefloor = 2.2235,
                adUnitId = "admob2",
                uid = "1",
            ),
        ),
        pricefloor = 0.01,
        token = null,
        externalWinNotificationsEnabled = true,
        auctionConfigurationUid = "10",
    )

    private val activity: Activity by lazy { mockk(relaxed = true) }
    private val adaptersSource: AdaptersSource = mockk()
    private val regulation: Regulation = mockk(relaxed = true)
    private val conductBiddingAuction: ConductBiddingRoundUseCase = mockk()
    private val conductNetworkAuction: ConductNetworkRoundUseCase = mockk()

    private val testee: ExecuteRoundUseCase by lazy {
        ExecuteRoundUseCaseImpl(
            adaptersSource = adaptersSource,
            regulation = regulation,
            conductBiddingAuction = conductBiddingAuction,
            conductNetworkAuction = conductNetworkAuction
        )
    }

    @Before
    fun before() {
        mockkObject(DeviceType)
        every { DeviceType.init(any()) } returns Unit
        DI.init(activity)
//        DI.setFactories()
        mockkLog()

        every { adaptersSource.adapters } returns setOf(
            TestAdapter(
                demandId = DemandId(Admob),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Succeed
                )
            ),
            TestBiddingAdapter(
                demandId = DemandId(BidMachine),
                testAdapterParameters = TestAdapterParameters(
                    bid = Process.Succeed,
                    fill = Process.Succeed
                )
            ),
        )
    }

    @After
    fun after() {
        unmockkAll()
    }

    @Ignore
    @Test
    fun `it should conduct round`() = runTest {
        // mockk results
        coEvery {
            conductNetworkAuction.invoke(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns NetworksResult(
            results = listOf(
                CoroutineScope(mainDispatcherOverridden!!).async {
                    AuctionResult.Network(
                        adSource = mockk<AdSource<*>>(relaxed = true).also {
                            every { it.demandId } returns DemandId(Admob)
                            every { it.ad } returns Ad(
                                demandAd = DemandAd(AdType.Interstitial),
                                networkName = "admob",
                                adUnitId = null,
                                roundId = "r123",
                                currencyCode = USD,
                                dsp = null,
                                ecpm = 1.3,
                                auctionId = "a123",
                                bidType = BidType.CPM,

                            )
                        },
                        roundStatus = RoundStatus.Successful
                    )
                }
            ),
            remainingLineItems = emptyList()
        )
        val auctionResult = AuctionResult.Bidding(
            adSource = mockk<AdSource<*>>(relaxed = true).also {
                every { it.demandId } returns DemandId(BidMachine)
                every { it.ad } returns Ad(
                    demandAd = DemandAd(AdType.Interstitial),
                    networkName = "bidmachine_network",
                    adUnitId = null,
                    roundId = "r123",
                    currencyCode = USD,
                    dsp = null,
                    ecpm = 2.3,
                    auctionId = "a123",
                    bidType = BidType.CPM,
                )
            },
            roundStatus = RoundStatus.Successful
        )
        coEvery {
            conductBiddingAuction.invoke(
                context = any(),
                biddingSources = any(),
                participantIds = any(),
                adTypeParam = any(),
                demandAd = any(),
                bidfloor = any(),
                auctionId = any(),
                round = any(),
                auctionConfigurationId = any(),
                auctionConfigurationUid = any(),
                resultsCollector = any()
            )
        } returns Unit

        // it should conduct round with 2 results
        val results = testee.invoke(
            demandAd = DemandAd(AdType.Interstitial),
            auctionResponse = auctionConfig,
            adTypeParam = AdTypeParam.Interstitial(activity, 1.0),
            round = RoundRequest(
                id = "round_1",
                timeoutMs = 15000,
                demandIds = listOf("Unknown_network1", Admob, "Unknown_network2"),
                biddingIds = listOf(BidMachine),
            ),
            roundIndex = 1,
            pricefloor = 0.4,
            lineItems = emptyList(),
            resultsCollector = mockk(relaxed = true),
            onFinish = { remainingLineItems ->
            }
        )
        results
            .onFailure {
                it.printStackTrace()
                error("unexpected")
            }
            .onSuccess {
                assertThat(it).hasSize(4)
                assertThat(it.filter { it.roundStatus == RoundStatus.UnknownAdapter }).hasSize(2)
                assertThat(it.filter { it.roundStatus == RoundStatus.Successful }).hasSize(2)
            }
    }
}