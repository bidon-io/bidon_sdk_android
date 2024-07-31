package org.bidon.sdk.auction.impl

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.bidon.sdk.adapter.AdaptersSource
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.usecases.ExecuteAuctionUseCase
import org.bidon.sdk.auction.usecases.RequestAdUnitUseCase
import org.bidon.sdk.auction.usecases.impl.ExecuteAuctionUseCaseImpl
import org.bidon.sdk.config.models.adapters.Process
import org.bidon.sdk.config.models.adapters.TestAdapter
import org.bidon.sdk.config.models.adapters.TestAdapterParameters
import org.bidon.sdk.config.models.adapters.TestBiddingAdapter
import org.bidon.sdk.config.models.auctions.impl.Admob
import org.bidon.sdk.config.models.auctions.impl.BidMachine
import org.bidon.sdk.config.models.base.ConcurrentTest
import org.bidon.sdk.mockkLog
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.utils.di.DI
import org.bidon.sdk.utils.json.jsonObject
import org.junit.After
import org.junit.Before
import org.junit.Ignore

/**
 * Created by Aleksei Cherniaev on 26/06/2023.
 */
@Ignore
internal class ExecuteAuctionUseCaseImplTest : ConcurrentTest() {

    private val auctionConfig = AuctionResponse(
        adUnits = listOf(
            AdUnit(
                demandId = "admob",
                label = "admob_banner",
                pricefloor = 0.25,
                uid = "12387837129819",
                bidType = BidType.CPM,
                timeout = 5000,
                ext = jsonObject { "ad_unit_id" hasValue "ca-app-pub-3940256099942544/6300978111" }.toString(),
            ),
            AdUnit(
                demandId = "bidmachine",
                label = "bidmachine_banner",
                uid = "32387837129819",
                pricefloor = 0.0,
                bidType = BidType.CPM,
                timeout = 5000,
                ext = null,
            )
        ),
        pricefloor = 0.01,
        auctionId = "auctionId_123",
        auctionConfigurationId = 10,
        auctionConfigurationUid = "10",
        externalWinNotificationsEnabled = true,
        auctionTimeout = 10000L,
        noBids = listOf(
            AdUnit(
                bidType = BidType.RTB,
                demandId = "dem7",
                label = "dem7_label",
                pricefloor = 0.021,
                uid = "123567",
                timeout = 5000L,
                ext = ""
            )
        )
    )

    private val activity: Activity by lazy { mockk(relaxed = true) }
    private val adaptersSource: AdaptersSource = mockk()
    private val regulation: Regulation = mockk(relaxed = true)
    private val requestAdUnit: RequestAdUnitUseCase = mockk()

    private val testee: ExecuteAuctionUseCase by lazy {
        ExecuteAuctionUseCaseImpl(
            adaptersSource = adaptersSource,
            regulation = regulation,
            requestAdUnit = requestAdUnit,
        )
    }

    @Before
    fun before() {
        mockkObject(DeviceInfo)
        every { DeviceInfo.init(any()) } returns Unit
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

//    @Test
//    fun `it should conduct round`() = runTest {
//        // mockk results
//        coEvery {
//            conductNetworkAuction.invoke(
//                any(),
//                any(),
//                any(),
//                any(),
//                any(),
//                any(),
//                any(),
//                any(),
//                any()
//            )
//        } returns NetworksResult(
//            results = listOf(
//                CoroutineScope(mainDispatcherOverridden!!).async {
//                    AuctionResult.Network(
//                        adSource = mockk<AdSource<*>>(relaxed = true).also {
//                            every { it.demandId } returns DemandId(Admob)
//                            every { it.ad } returns Ad(
//                                demandAd = DemandAd(AdType.Interstitial),
//                                roundId = "r123",
//                                currencyCode = USD,
//                                dsp = null,
//                                ecpm = 1.3,
//                                auctionId = "a123",
//                                adUnit = AdUnit(
//                                    demandId = "admob",
//                                    label = "admob_banner",
//                                    pricefloor = 0.25,
//                                    uid = "12387837129819",
//                                    bidType = BidType.CPM,
//                                    ext = jsonObject { "ad_unit_id" hasValue "ca-app-pub-3940256099942544/6300978111" }.toString(),
//                                )
//                            )
//                        },
//                        roundStatus = RoundStatus.Successful
//                    )
//                }
//            ),
//            remainingAdUnits = emptyList()
//        )
//        coEvery {
//            conductBiddingAuction.invoke(
//                context = any(),
//                biddingSources = any(),
//                adTypeParam = any(),
//                demandAd = any(),
//                bidfloor = any(),
//                auctionId = any(),
//                auctionConfigurationId = any(),
//                auctionConfigurationUid = any(),
//                adUnits = any(),
//                resultsCollector = any(),
//            )
//        } returns Unit
//
//        // it should conduct round with 2 results
//        val results = testee.invoke(
//            demandAd = DemandAd(AdType.Interstitial),
//            auctionResponse = auctionConfig,
//            adTypeParam = AdTypeParam.Interstitial(activity, 1.0, "auctionKey"),
//            pricefloor = 0.4,
//            adUnits = emptyList(),
//            resultsCollector = mockk(relaxed = true)
//        )
//        results
//            .onFailure {
//                it.printStackTrace()
//                error("unexpected")
//            }
//            .onSuccess {
//                assertThat(it).hasSize(4)
//                assertThat(it.filter { it.roundStatus == RoundStatus.UnknownAdapter }).hasSize(2)
//                assertThat(it.filter { it.roundStatus == RoundStatus.Successful }).hasSize(2)
//            }
//    }
}