package org.bidon.sdk.auction.usecases.impl

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.adapter.impl.AdaptersSourceImpl
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.ads.banner.helper.DeviceInfo
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.impl.MaxEcpmAuctionResolver
import org.bidon.sdk.auction.impl.ResultsCollectorImpl
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.ExecuteRoundUseCase
import org.bidon.sdk.auction.usecases.models.RoundResult
import org.bidon.sdk.config.models.adapters.Process
import org.bidon.sdk.config.models.adapters.TestAdapter
import org.bidon.sdk.config.models.adapters.TestAdapterParameters
import org.bidon.sdk.config.models.adapters.TestInterstitialImpl
import org.bidon.sdk.mockkLog
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.regulation.Iab
import org.bidon.sdk.regulation.Regulation
import org.bidon.sdk.utils.di.DI
import org.junit.Before
import org.junit.Test

/**
 * Created by Aleksei Cherniaev on 17/11/2023.
 */
class ConductNetworkRoundUseCaseImplTest {

    private val activity: Activity = mockk(relaxed = true, relaxUnitFun = true)
    private val demandId = "test_adapter"

    private val testee by lazy {
        ConductNetworkRoundUseCaseImpl()
    }

    @Before
    fun before() {
        mockkObject(DeviceInfo)
        every { DeviceInfo.init(any()) } returns Unit
        DI.init(activity)
        mockkLog()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `it should fail when result is below pricefloor`() = runTest {
        val demandAd = DemandAd(AdType.Interstitial)
        val resultsCollector: ResultsCollector = ResultsCollectorImpl(MaxEcpmAuctionResolver)
        val roundRequest = RoundRequest(
            id = "id123",
            timeoutMs = 1000,
            biddingIds = listOf("unknown"),
            demandIds = listOf(demandId),
        )
        resultsCollector.startRound(roundRequest, 10.0)
        val result = testee.invoke(
            context = activity,
            networkSources = listOf(
                TestInterstitialImpl(
                    testParameters = TestAdapterParameters(
                        bid = Process.Succeed,
                        fill = Process.Succeed,
                        resultPricefloor = 0.5
                    )
                )
            ),
            participantIds = listOf(demandId),
            adTypeParam = AdTypeParam.Interstitial(
                activity = activity,
                pricefloor = 10.0,
            ),
            demandAd = demandAd,
            lineItems = listOf(
                LineItem(
                    uid = "2387498327489279",
                    demandId = demandId,
                    pricefloor = 12.0,
                    adUnitId = "adUnitId",
                )
            ),
            round = roundRequest,
            pricefloor = 1.2,
            scope = this,
            resultsCollector = resultsCollector
        )
        advanceUntilIdle()
        val a = result.results.map {
            it.await()
        }
        advanceUntilIdle()
        println("--> $a")
        val roundResults = resultsCollector.getRoundResults()
        println("->> $roundResults")
        require(roundResults is RoundResult.Results)
//        assertThat(roundResults.networkResults).hasSize(1)
        error("Not implemented")
    }

    @Test
    fun `execute round`() = runTest {
        val executeRound: ExecuteRoundUseCase = ExecuteRoundUseCaseImpl(
            conductNetworkAuction = testee,
            adaptersSource = AdaptersSourceImpl().apply {
                this.add(listOf(getTestAdapter()))
            },
            conductBiddingAuction = mockk(relaxed = true),
            regulation = object : Regulation {
                override var gdpr: Gdpr = Gdpr.DoesNotApply
                override var gdprConsentString: String? = null
                override val gdprApplies: Boolean = false
                override val hasGdprConsent: Boolean = false
                override var usPrivacyString: String? = null
                override val ccpaApplies: Boolean = false
                override val hasCcpaConsent: Boolean = false
                override var coppa: Coppa = Coppa.No
                override val iab: Iab = Iab(
                    tcfV1 = null,
                    tcfV2 = null,
                    usPrivacy = null
                )

            },
        )
        val roundRequest = RoundRequest(
            id = "id123",
            timeoutMs = 1000,
            biddingIds = listOf("unknown"),
            demandIds = listOf(demandId),
        )
        executeRound.invoke(
            demandAd = DemandAd(AdType.Interstitial),
            adTypeParam = AdTypeParam.Interstitial(
                activity = activity,
                pricefloor = 10.0,
            ),
            lineItems = listOf(
                LineItem(
                    uid = "2387498327489279",
                    demandId = demandId,
                    pricefloor = 12.0,
                    adUnitId = "adUnitId",
                )
            ),
            round = RoundRequest(
                id = "id123",
                timeoutMs = 1000,
                biddingIds = listOf("unknown"),
                demandIds = listOf(demandId),
            ),
            pricefloor = 10.0,
            resultsCollector = mockk(relaxed = true),
            roundIndex = 64,
            auctionResponse = AuctionResponse(
                rounds = listOf(roundRequest),
                pricefloor = 10.0,
                token = null,
                auctionConfigurationUid = "489753324234229738",
                lineItems = listOf(
                    LineItem(
                        uid = "2387498327489279",
                        demandId = demandId,
                        pricefloor = 12.0,
                        adUnitId = "adUnitId",
                    )
                ),
                externalWinNotificationsEnabled = false,
                auctionConfigurationId = 12,
                auctionId = "auctionId",
            ),
            onFinish = { remainingLineItems ->
                println("on finish: remainingLineItems = $remainingLineItems")
            }
        ).onSuccess {
            println(it)
            error("onSuccess")
        }.onFailure {
            println("onFailure")
            it.printStackTrace()
        }
        error("end")
    }


    private fun getTestAdapter() = TestAdapter(
        DemandId(demandId), TestAdapterParameters(
            bid = Process.Succeed,
            fill = Process.Succeed,
            resultPricefloor = 2993.0
        )
    )
}